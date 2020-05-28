//Authors: James Adhitthana,  Andre Kurnia, Christopher Yefta, Deananda Irwansyah//

//------------------------Initialize Firebase------------------------//
var config = {
  apiKey: "GETYOUROWNKEY",
  authDomain: "GETYOUROWNKEY",
  databaseURL: "GETYOUROWNKEY",
  projectId: "GETYOUROWNKEY",
  storageBucket: "GETYOUROWNKEY",
  messagingSenderId: "GETYOUROWNKEY"
};
firebase.initializeApp(config);
//-END OF: Initialize Firebase-//

//------------------------JadyTrack Firebase Settings------------------------//
var db = firebase.database();
function logError(err) {
  console.log(err)
}


//------------------------New Auth------------------------//
var auth = firebase.auth();
//--Listener that checks if there is a change in the condition--//
auth.onAuthStateChanged(function (user) {
  if (user) { //If a user is logged in
    //List of user variables: name, email, photoUrl, uid, emailVerified;
    console.log(user.email + " (" + user.uid + ") is logged in.")
    // document.getElementById("labelCurrentUser").innerHTML = user.email;
    // window.location.href = "account/account.html"; //redirect user if not logged in.
  } else { //If no user
    console.log("No user is logged in")
    alert("Please log-in to access the web-app")
    // document.getElementById("labelCurrentUser").innerHTML = "empty";
  }
})
//...Continued in jadytrackmaps.js//