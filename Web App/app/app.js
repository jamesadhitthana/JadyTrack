//-Languages-//
var selectedLanguage = 'en'; //GLOBAL LANGUAGE SELECTOR
var finalTrackingId = null;
const languageList = {
    'id': [`Selamat datang di JadyTrack Web`
        , `"Tahu apa yang terjadi, jangan khawatir tentang ..."`
        , `Mulai Melacak`
        , `Masukkan Tracking ID`
        , `Masukkan Tracking ID Anda di sini`
        , `Tracking ID (Contoh: "`
        , `Lacak ID`
        , `Lihat Peta`
        , `ID Sesi Saat Ini:`
        , `Jumlah Koordinat`
        , `ID Target:`
        , `Nama Target:`
        , `Telah tiba:`
        , `Status Check In:`
        , `Di dalam Geofence:`
        , `Status SOS:`
        , `Tautan Kedaluwarsa:`
        , `Hak Cipta 2020. All Rights Reserved.Dikembangkan oleh`],
    'en': [`Welcome to JadyTrack Web`
        , `"Know what's going on, dont worry about..."`
        , `Start Tracking`
        , `Input Tracking ID`
        , `Input your Tracking ID here`
        , `Tracking ID (For example: "`
        , `Submit ID`
        , `View Map`
        , `Current Session ID:`
        , `Number of Coordinates:`
        , `Target ID:`
        , `Target Name:`
        , `Has Arrived:`
        , `Check In Status:`
        , `Inside Geofence:`
        , `Status SOS:`
        , `Link Expired:`
        , `Copyright 2020. All Rights Reserved. Developed by`]
}


// *--Runner--* //
const EXAMPLETRACKINGID = '-L_tTv8ulabhy1vPOdo0'
const boolShowCoordinatesInHamburgerMenu = false;

// document.getElementById("logoutButton").addEventListener("click", function () {
//     alertConfirmLogout();
// });
document.getElementById("flagBahasaIndonesia").addEventListener("click", function () {
    event.preventDefault();
    flagToggleLanguage('id')
});
document.getElementById("flagEnglish").addEventListener("click", function () {
    event.preventDefault();
    flagToggleLanguage('en')
});

function flagToggleLanguage(languageFlag) {
    if (finalTrackingId == null) {
        selectLanguage(languageFlag) //'id' or 'en'
        iziToast.success({ message: "Updated language" });
    } else {
        window.location.href = `index.html?id=${finalTrackingId}&lang=${languageFlag}`
    }


}

document.getElementById("buttonClearTrackingId").addEventListener("click", function () {
    // window.location.href = `/app/index.html#textInputTrackingId`

    if (finalTrackingId == null) {
        document.location.reload(true)
    } else {
        window.location.href = `/app/index.html#textInputTrackingId`
    }
});
document.getElementById("buttonExampleTrackingId").addEventListener("click", function (e) {
    e.preventDefault();
    window.location.href = `/app/index.html?id=${EXAMPLETRACKINGID}`
    //! DEPRECEATED: Sample using real tracking ID
    // document.getElementById("inputSessionID").innerHTML = EXAMPLETRACKINGID;
    // loadSelectedTrackingId(EXAMPLETRACKINGID)
});
loadSearchedURLParams()
jQuery("#subTitleJady").fitText(1.2, { minFontSize: '5px', maxFontSize: '25px' });



// *---URL PARAMS ---//
function loadSearchedURLParams() {
    //--Load searched URL Params--//
    let urlParams = new URLSearchParams(window.location.search);

    //Language
    let langUrlParams = urlParams.get('lang')
    if (langUrlParams != null && langUrlParams != "") {
        if (langUrlParams.trim() != "") {
            console.log("Changing Language using url params", langUrlParams);
            // Change Language
            selectLanguage(langUrlParams)
        }

    } else {
        // Check if Cookies exist for language changer if there is no valid lang urlParams
        let userCookieSelectedLanguage = Cookies.get('userSelectedLanguage');
        if (userCookieSelectedLanguage != undefined) {
            selectLanguage(userCookieSelectedLanguage);
        }
    }

    //Tracking ID
    searchedUrlParams = urlParams.get('id')
    if (searchedUrlParams != null && searchedUrlParams != "") {
        if (searchedUrlParams.trim() != "") {
            console.log("Using querystring id: " + searchedUrlParams);
            // iziToast.info({ title: "Loading Tracking ID:", message: searchedUrlParams });//Reminder
            document.getElementById("inputSessionID").innerHTML = searchedUrlParams
            loadSelectedTrackingId(searchedUrlParams)
        }

    }
}

function disableInputTrackingId() {
    document.getElementById("inputSessionID").readOnly = true;
    // document.getElementById("buttonSubmitTrackingId").disabled = true;
    document.getElementById("divButtonSubmitTrackingId").style.display = "none";
}

//* ----Functions----- //
function alertFailure(errorDescription, errorLog) {
    try {
        Swal.fire('Oops!', `${errorDescription}`, 'error')
    } catch (err) {
        console.error("Failed to show alertFailure() using SweetAlert")
    }
    console.error('[alertFailure]', `${errorDescription}`, errorLog)
    try {
        iziToast.error({ message: `${errorDescription}` })
    } catch (err) {
        console.error("Failed to show alertFailure() using iziToast")
    }

}

function notifyPages(mode) {
    try {
        var audio = new Audio('notification.wav');
        audio.play();
    } catch (err) {
        console.warn("Gagal nih playing audio (biasanya sih karena di blok sama browsernya)", err)
    }

    switch (mode) {
        case 'exitGeofence':
            // Indonesian//
            if (selectedLanguage == 'id') {
                window.open("notify-in-geofence-id.html", "Rekan Anda keluar geofence! | JadyTrack", '_blank');
                swal.fire(
                    {
                        title: `Rekan Anda keluar geofence!`,
                        html: "Rekan Anda telah <b>keluar geofence!</b><br>Anda mungkin ingin memeriksa dengan rekan Anda. <br> (ini adalah pesan otomatis)",
                        icon: 'warning',
                        allowOutsideClick: false,
                        allowEscapeKey: false,
                        allowEnterKey: false
                    });
                Push.create("Rekan Anda keluar geofence! | JadyTrack", {
                    body: "Rekan Anda telah keluar geofence! Anda mungkin ingin memeriksa dengan rekan Anda (ini adalah pesan otomatis)",
                    icon: 'images/jadyicon.png',
                    timeout: 4000,
                    onClick: function () {
                        window.focus();
                        this.close();
                    }
                });

            }
            // English//
            else {
                window.open("notify-in-geofence-en.html", "Your peer has exited the geofence! | JadyTrack", '_blank');
                swal.fire(
                    {
                        title: `Your peer has exited the geofence!`,
                        html: "Your peer has <b>exited the geofence!</b><br>You might want to check up with your peer.<br>(this is an automated message)",
                        icon: 'warning',
                        allowOutsideClick: false,
                        allowEscapeKey: false,
                        allowEnterKey: false
                    });
                Push.create("Your peer has exited the geofence! | JadyTrack", {
                    body: "Your peer has exited the geofence! You might want to check up with your peer (this is an automated message)",
                    icon: 'images/jadyicon.png',
                    timeout: 4000,
                    onClick: function () {
                        window.focus();
                        this.close();
                    }
                });
            }

            break;
        case 'arrived':
            // Indonesian//
            if (selectedLanguage == 'id') {
                window.open("notify-has-arrived-id.html", "Rekan Anda telah tiba! | JadyTrack", '_blank');
                swal.fire("Rekan Anda telah tiba", 'Rekan Anda telah tiba di tujuan. <br> (ini adalah pesan otomatis)', 'success')
                Push.create("Rekan Anda telah tiba! | JadyTrack", {
                    body: "Rekan Anda telah tiba di tujuan (ini adalah pesan otomatis)",
                    icon: 'images/jadyicon.png',
                    timeout: 4000,
                    onClick: function () {
                        window.focus();
                        this.close();
                    }
                });
            }
            // English//
            else {
                window.open("notify-has-arrived-en.html", "Your peer has arrived! | JadyTrack", '_blank');
                swal.fire("Your peer has arrived", 'Your peer has arrived to the destination.<br>(this is an automated message)', 'success')
                Push.create("Your peer has arrived! | JadyTrack", {
                    body: "Your peer has arrived to the destination (this is an automated message)",
                    icon: 'images/jadyicon.png',
                    timeout: 4000,
                    onClick: function () {
                        window.focus();
                        this.close();
                    }
                });
            }
            break;
        case 'manualCheckIn':
            // Indonesian//
            if (selectedLanguage == 'id') {
                window.open("notify-has-arrived-id.html", "Rekan Anda telah check-in secara manual! | JadyTrack", '_blank');
                swal.fire("Rekan Anda telah check-in secara manual!", 'Rekan Anda telah check in ke tujuan.', 'success')
                Push.create("Rekan Anda telah check-in secara manual! | JadyTrack", {
                    body: "Rekan Anda telah check in ke tujuan",
                    icon: 'images/jadyicon.png',
                    timeout: 4000,
                    onClick: function () {
                        window.focus();
                        this.close();
                    }
                });
            }
            // English//
            else {
                window.open("notify-has-arrived-en.html", "Your peer has manually check-in! | JadyTrack", '_blank');
                swal.fire("Your peer checked-in", 'Your peer has checked in to your destinaion.', 'success')
                Push.create("Your peer has manually check-in! | JadyTrack", {
                    body: "Your peer has checked in to your destinaion",
                    icon: 'images/jadyicon.png',
                    timeout: 4000,
                    onClick: function () {
                        window.focus();
                        this.close();
                    }
                });
            }
            break;
        case 'sos':
            // Indonesian//
            if (selectedLanguage == 'id') {
                window.open("notify-sos-id.html", "TOMBOL DARURAT TELAH DITEKAN | JadyTrack", '_blank');
                swal.fire(
                    {
                        title: `Tombol Darurat Diaktifkan`,
                        html: "Rekan Anda <b> menekan TOMBOL DARURAT </b> <br> Anda mungkin ingin memeriksa dengan rekan Anda.",
                        icon: 'error',
                        allowOutsideClick: false,
                        allowEscapeKey: false,
                        allowEnterKey: false
                    });
                Push.create("TOMBOL DARURAT TELAH DITEKAN | JadyTrack", {
                    body: "Rekan Anda menekan TOMBOL DARURAT. Anda mungkin ingin memeriksa dengan rekan Anda.",
                    icon: 'images/jadyicon.png',
                    timeout: 4000,
                    onClick: function () {
                        window.focus();
                        this.close();
                    }
                });
            }
            // English//
            else {
                window.open("notify-sos-en.html", "EMERGENCY BUTTON PRESSED | JadyTrack", '_blank');
                swal.fire(
                    {
                        title: `Emergency Button Activated`,
                        html: "Your peer <b>pressed the EMERGENCY BUTTON</b><br>You might want to check up with your peer.",
                        icon: 'error',
                        allowOutsideClick: false,
                        allowEscapeKey: false,
                        allowEnterKey: false
                    });
                Push.create("EMERGENCY BUTTON PRESSED | JadyTrack", {
                    body: "Your peer pressed the EMERGENCY BUTTON! You might want to check up with your peer.",
                    icon: 'images/jadyicon.png',
                    timeout: 4000,
                    onClick: function () {
                        window.focus();
                        this.close();
                    }
                });
            }
            break;
        default:
            console.error("Failed to notify something!")
            break;
    }
}

function welcomeToJadyTrack() {
    Swal.fire({
        title: `Hello ${currentUser.displayName}!`,
        html: "It seems like you are new here. <br>You have not selected your default page.<br> Clicking OK will send you to the settings page.",
        icon: 'info',
        allowOutsideClick: false,
        allowEscapeKey: false,
        allowEnterKey: false
    }).then((result) => {
        if (result.value) {
            //Redirecting
            try {
                window.location.href = "settings.html"
            } catch (err) {
                alertFailure("Failed to redirect to  settings page")
            }
        }
    });
}

// --Login & Logout-- //
function alertNotLoggedIn() {
    Swal.fire({
        title: `Members Only!`,
        html: "Please log-in to access the web-app",
        icon: 'info',
        confirmButtonText: "Login",
        allowOutsideClick: false,
        allowEscapeKey: false,
        allowEnterKey: false
    }).then((result) => {
        if (result.value) {
            //Redirecting
            try {
                window.location.href = "index.html"
            } catch (err) {
                alertFailure("Failed to redirect to teacher settings page")
            }
        }
    });
}

function alertConfirmLogout() {
    Swal.fire({
        title: `Logout?`,
        html: "Are you sure you want to logout?",
        icon: 'question',
        confirmButtonText: "Logout",
        showCancelButton: true
    }).then((result) => {
        if (result.value) {
            //Redirecting
            try {
                handleLogout()
            } catch (err) {
                alertFailure("Failed to logout user")
            }
        }
    });
}
//--//
function handleLogout() {
    console.log('pressed logout button');
    //Sign Out/Logouts the user
    auth.signOut()
        .then(function (data) {
            console.log("--Logged Out--")
            console.log(data)
            iziToast.success({ title: "Logged out successfully", message: "See you soon!" });
            setInterval(function () {
                //redirect user to home
                window.location.href = '../index.html'
            }, 1500);
        })
        .catch(function (err) {
            console.log(err)
            alert(err)
        })
}




//? --Languages --//
function selectLanguage(language) {
    // * Language Selector
    switch (language) {
        case "id":
            selectedLanguage = "id"
            console.log("Changing language to Bahasa Indonesian (id)");
            break;
        default:
            selectedLanguage = "en"
            console.log("Changing language to DEFAULT ENGLISH because the detected language is:", language);
            break;
    }

    // *Element Language Changer
    console.group("Changing Languages");
    elementLanguage('headerWelcomeToJadyTrack', selectedLanguage, 0);
    elementLanguage('subTitleJady', selectedLanguage, 1);
    elementLanguage('textStartTracking', selectedLanguage, 2);
    elementLanguage('textInputTrackingId', selectedLanguage, 3);
    elementLanguage('textInputTrackingIdHere', selectedLanguage, 4);
    elementLanguage('textTrackingIdExample', selectedLanguage, 5);
    elementLanguage('buttonSubmitTrackingId', selectedLanguage, 6);
    elementLanguage('textViewMap', selectedLanguage, 7);
    elementLanguage('titleInput', selectedLanguage, 8);
    elementLanguage('titleNumHistory', selectedLanguage, 9);
    elementLanguage('titleTargetId', selectedLanguage, 10);
    elementLanguage('titleTargetName', selectedLanguage, 11);
    elementLanguage('titleHasArrived', selectedLanguage, 12);
    elementLanguage('titleManualCheckIn', selectedLanguage, 13);
    elementLanguage('titleInsideGeofence', selectedLanguage, 14);
    elementLanguage('titleStatusSOS', selectedLanguage, 15);
    elementLanguage('titleLinkExpired', selectedLanguage, 16);
    elementLanguage('textCopyrightJady', selectedLanguage, 17);
    console.groupEnd();

    // User Selected Language Cookies:
    Cookies.set('userSelectedLanguage', language)

}

function elementLanguage(elementId, languageChosen, indexArray) {
    try {
        document.getElementById(elementId).innerHTML = languageList[languageChosen][indexArray];
        console.log("Successfuly changed language for", elementId);
    } catch (err) {
        console.error("Failed to chang language for", elementId, err);
    }
}

//! ---------------DEPRECEATED--------------- //
//--Update Web Page with Database Info--//
function showUserData(items) {
    console.log("User data according to UID database nih");
    console.log(items.val());

    if (items.val() != null) {
        document.getElementById("labelCurrentUserName").innerHTML = items.val().name
    } else {
        console.log("Can't find UID");
        document.getElementById("labelCurrentUserName").innerHTML = "Can't find User ID"
    }
}

function showError(err) {
    console.log(err)
}