//Authors: James Adhitthana,  Andre Kurnia, Christopher Yefta, Deananda Irwansyah//

//------------------------Initialize Firebase------------------------//
// API Key moved to jady-api-key.js
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
  //If a user is logged in
  if (user) {
    //List of user variables: name, email, photoUrl, uid, emailVerified;
    console.log(user.email + " (" + user.uid + ") is logged in.")
    // iziToast.success({ message: `Welcome back ${user.email}` })
    if(selectedLanguage=='id'){
      iziToast.info({ title: "Jangan Lupa:", message: "Pastikan untuk mengaktifkan notifikasi dan mengaktifkan popup untuk mendapatkan notifikasi real-time." });//Reminder
    }else{
      iziToast.info({ title: "Reminder:", message: "Make sure to enable notifications and enable popups to get real-time notifications." });//Reminder
    }
    

  }
  //If no user
  else {
    console.log("No user is logged in")
  }
})
//...Continued in jadytrackmaps.js//