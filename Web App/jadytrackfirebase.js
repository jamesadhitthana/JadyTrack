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
//...Continued in jadytrackmaps.js//