package com.example.firebaseemaillogin

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.facebook.CallbackManager
import com.facebook.AccessToken
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.facebook.login.widget.LoginButton
import com.facebook.login.LoginManager
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.squareup.picasso.Picasso

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    val facebookUserId  = ""
    private val TAG = "FacebookLGoogleActivity"
    private var auth: FirebaseAuth? = null
    private lateinit var callbackManager: CallbackManager
//    var authListener: FirebaseAuth.AuthStateListener? = null
    private val RC_SIGN_IN = 9001
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        buttonFacebookSignout.setOnClickListener(this)
        signInButton.setOnClickListener(this)
        signOutButton.setOnClickListener(this)
        disconnectButton.setOnClickListener(this)

//        Glide.with(Activity activity)
        // [START config_signin]
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.request_client_id))
            .requestEmail()
            .build()
        // [END config_signin]
        googleSignInClient = GoogleSignIn.getClient(this, gso)


        auth = FirebaseAuth.getInstance();


        callbackManager = CallbackManager.Factory.create()

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        buttonFacebookLogin.setReadPermissions("email", "public_profile")
        buttonFacebookLogin.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d(TAG, "facebook:onSuccess:$loginResult")

                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {

                Log.d(TAG, "facebook:onCancel")

                // [START_EXCLUDE]

                updateUI(null)

                // [END_EXCLUDE]

            }
            override fun onError(error: FacebookException) {

                Log.d(TAG, "facebook:onError", error)

                // [START_EXCLUDE]

                updateUI(null)

                // [END_EXCLUDE]

            }

        })

    }

    public override fun onStart() {

        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth!!.currentUser

    }

    // [END on_start_check_user]



    // [START on_activity_result]

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);

        if (requestCode == RC_SIGN_IN) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {

                // Google Sign In was successful, authenticate with Firebase

                val account = task.getResult(ApiException::class.java)

                firebaseAuthWithGoogle(account!!)

            } catch (e: ApiException) {

                // Google Sign In failed, update UI appropriately

                Log.w(TAG, "Google sign in failed", e)

                // [START_EXCLUDE]

                updateUI2(null)

                // [END_EXCLUDE]

            }

        }


    }

    // [END on_activity_result]



    // [START auth_with_facebook]

    private fun handleFacebookAccessToken(token: AccessToken) {

        Log.d(TAG, "handleFacebookAccessToken:$token")


        val credential = FacebookAuthProvider.getCredential(token.token)


        auth!!.signInWithCredential(credential)

            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    // Sign in success, update UI with the signed-in user's information

                    Log.d(TAG, "signInWithCredential:success")

                    val user = auth!!.currentUser

                    updateUI(user)

                } else {

                    // If sign in fails, display a message to the user.

                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                    Toast.makeText(baseContext, "Authentication failed.",

                        Toast.LENGTH_SHORT).show()

                    updateUI(null)

                }

            }

    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {

        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)

        auth!!.signInWithCredential(credential)

            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    // Sign in success, update UI with the signed-in user's information

                    Log.d(TAG, "signInWithCredential:success")

                    val user = auth!!.currentUser

                    updateUI2(user)

                } else {

                    // If sign in fails, display a message to the user.

                    Log.w(TAG, "signInWithCredential:failure", task.exception)

//                    Snackbar.make(main_layout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()

                    updateUI2(null)

                }


            }

    }

    // [END auth_with_facebook]
    private fun signIn() {

        val signInIntent = googleSignInClient.signInIntent

        startActivityForResult(signInIntent, RC_SIGN_IN)

    }


    fun signOut() {

        auth!!.signOut()
        LoginManager.getInstance().logOut()
        updateUI(null)

        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this) {
            updateUI2(null)

        }

    }

    private fun revokeAccess() {
        // Firebase sign out
        auth!!.signOut()

        // Google revoke access

        googleSignInClient.revokeAccess().addOnCompleteListener(this) {

            updateUI2(null)

        }

    }



    private fun updateUI(user: FirebaseUser?) {

        if (user != null) {

            val Url= auth!!.currentUser!!.photoUrl

            iv_profile.visibility=View.VISIBLE

            Glide.with(this).load(Url).into(iv_profile);

            status.text = getString(R.string.facebook_status_fmt, user.displayName)

            detail.text = getString(R.string.firebase_status_fmt, user.uid)

            buttonFacebookLogin.visibility = View.GONE

            buttonFacebookSignout.visibility = View.VISIBLE

            signInButton.visibility = View.GONE


        } else {

            status.setText(R.string.signed_out)

            detail.text = null



            buttonFacebookLogin.visibility = View.VISIBLE

            buttonFacebookSignout.visibility = View.GONE

            iv_profile.visibility=View.GONE
        }

    }

    private fun updateUI2(user: FirebaseUser?) {

        if (user != null) {

            val Url= auth!!.currentUser!!.photoUrl

            iv_profile.visibility=View.VISIBLE

            Glide.with(this).load(Url).into(iv_profile);

            status.text = getString(R.string.google_status_fmt, user.email)

            detail.text = getString(R.string.firebase_status_fmt, user.uid)

            signInButton.visibility = View.GONE

            signOutAndDisconnect.visibility = View.VISIBLE

            buttonFacebookLogin.visibility = View.GONE



        } else {

            status.setText(R.string.signed_out)

            detail.text = null


            signInButton.visibility = View.VISIBLE

            signOutAndDisconnect.visibility = View.GONE

            iv_profile.visibility=View.GONE
        }

    }


    override fun onClick(v: View) {

        val i = v.id
        if (i == R.id.buttonFacebookSignout) signOut()
        if (i == R.id.signInButton) signIn()
        if (i == R.id.signOutButton) signOut()
        if (i == R.id.disconnectButton) revokeAccess()

        }

    }

