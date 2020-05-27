//Authors: James Adhitthana,  Andre Kurnia, Christopher Yefta, Deananda Irwansyah//
//TODO: Fix bugs for map markers not clearing when a new tracking id is added

//------------------------Variables------------------------//
var inputSessionID; //contoh: "-L_tTv8ulabhy1vPOdo0"
var numHistory;
var targetId;
var targetName;

var linkExpired;
var hasArrived;
var statusSOS;
var statusInGeofence;
var manualCheckIn;

var geofenceCoordinates = [];
var sessionCoordinates = []; //Array containing = [lat,long,time]
var calendarMonths = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
//END OF:Variables-----//

//------------------------Create Map in WebPage------------------------//
var jadyMap = new google.maps.Map(document.getElementById('map'), {
  zoom: 8, //8x zoom
  center: new google.maps.LatLng(-6.9, 108.4), //Default Map Location on Java Island (jawa barat area)
  mapTypeId: google.maps.MapTypeId.ROADMAP //make roadmap type for the GoogleMaps
});
bounds = new google.maps.LatLngBounds()
var infowindow = new google.maps.InfoWindow(); //make info window
//END OF:Create Map in webpage-----//


//------------------------Button and Firebase------------------------//
function myFunction() {
  if (document.getElementById("inputSessionID").value == "") {
    alert("Please fill in the Tracking ID in the textbox!");

  } else {

    inputSessionID = document.getElementById("inputSessionID").value; //get value from textarea

    //--Load Session Parent--//
    var dbTrackingSession = db.ref('trackingSession/' + inputSessionID);
    dbTrackingSession.on('value', showTrackingSessionData, logError);

    function showTrackingSessionData(items) {
      console.log("session Data parent nih");
      console.log(items.val()); //contoh ambil sessionID specific: "-L_tDIFtcLQxppYhCw5z"

      if (items.val() != null) {
        numHistory = items.val().numHistory
        targetId = items.val().targetId
        targetName = items.val().targetName
        //Update page labels
        updateLabels();

        //---
      } else {
        console.log("[PARENT Data] Can't find tracking ID");
        //Set page labels  as error
        updateLabelsAsError();
        //Refreshes page after that

      }
    }

    //--Load Session Child--//
    //testing Notifications db:       -Lb1YvHEhQtgZB9WM4P2
    var dbNotificationStatusLinkExpired = db.ref('trackingSession/' + inputSessionID + "/notifications/statusLinkExpired");
    dbNotificationStatusLinkExpired.on('value', notifyStatusLinkExpired, logError);

    function notifyStatusLinkExpired(items) {
      console.log("statusLinkExpired: " + items.val());
      if (items.val() == true) {//Currently link expired or not
        linkExpired = true;
        alert("LINK EXPIRED | JadyTrack");
      } else {
        linkExpired = false;
      }
    }

    var dbNotificationStatusInGeofence = db.ref('trackingSession/' + inputSessionID + "/notifications/statusInGeofence");
    dbNotificationStatusInGeofence.on('value', notifyStatusInGeofence, logError);

    function notifyStatusInGeofence(items) {
      console.log("statusInGeofence: " + items.val());
      if (items.val() == false) {//TARGET EXITED Geofence function
        statusInGeofence = false;
        window.open("notify-in-geofence.html", "Your peer has exited the geofence! | JadyTrack", '_blank');
        //alert("Your peer has exited the geofence! | JadyTrack");
      } else {
        statusInGeofence = true;
      }
    }

    var dbNotificationHasArrived = db.ref('trackingSession/' + inputSessionID + "/notifications/statusHasArrived");
    dbNotificationHasArrived.on('value', notifyHasArrived, logError);

    function notifyHasArrived(items) {
      console.log("hasArrived: " + items.val());
      if (items.val() == true) {//TARGET ARRIVED at destination
        hasArrived = true;
        window.open("notify-has-arrived.html", "Your peer has arrived! | JadyTrack", '_blank');
        //alert("Your peer has arrived! | JadyTrack");
      } else {
        hasArrived = false;
      }
    }

    var dbNotificationManualCheckIn = db.ref('trackingSession/' + inputSessionID + "/notifications/manualCheckIn");
    dbNotificationManualCheckIn.on('value', notifyManualCheckIn, logError);

    function notifyManualCheckIn(items) {
      console.log("manualCheckIn: " + items.val());
      if (items.val() == true) {//TARGET has enabled the manual Check In button
        manualCheckIn = true;
        console.log("MANUAL CHECK IN: Overriding the hasArrived boolean and calling the window open");

        hasArrived = true;
        window.open("notify-has-arrived.html", "Your peer has arrived! | JadyTrack", '_blank');
      } else {
        manualCheckIn = false;
      }
    }

    var dbNotificationStatusSOS = db.ref('trackingSession/' + inputSessionID + "/notifications/statusSOS");
    dbNotificationStatusSOS.on('value', notifyStatusSOS, logError);

    function notifyStatusSOS(items) {
      console.log("statusSOS: " + items.val());
      if (items.val() == true) {//TARGET has enabled the SOS button
        statusSOS = true;
        window.open("notify-sos.html", "EMERGENCY BUTTON PRESSED | JadyTrack", '_blank');
        //alert("EMERGENCY BUTTON PRESSED | JadyTrack");
      } else {
        statusSOS = false;
      }
    }


    var dbTrackingSessionLocations = db.ref('trackingSession/' + inputSessionID + "/locationHistory");
    dbTrackingSessionLocations.on('value', showTrackingSessionLocations, logError);

    function showTrackingSessionLocations(items) {
      console.log("session Data child nih"); //contoh ambil sessionID specific: "-L_tDIFtcLQxppYhCw5z"
      console.log(items.val());

      if (items.val() != null) {
        var topList = '';

        //clear previous array before pushing new stuff//
        sessionCoordinates = [];

        items.forEach(function (child) {
          topList += "<li>" + child.val().latitude + " - " + child.val().longitude + " - " + child.val().time + "</li>";

          //coba push ke array ini
          sessionCoordinates.push([child.val().time, child.val().latitude, child.val().longitude]); //coba push 
          UpdateMapJady();
        })

        document.getElementsByTagName('ul')[0].innerHTML = topList;

        //--Create Polyline for the targetPath--//
        var counterPolyLine;
        var polyLineTarget = [];

        for (counterPolyLine = 0; counterPolyLine < sessionCoordinates.length; counterPolyLine++) {
          polyLineTarget.push({ lat: sessionCoordinates[counterPolyLine][1], lng: sessionCoordinates[counterPolyLine][2] });
        }
        console.log(polyLineTarget);

        var targetPath = new google.maps.Polyline({
          path: polyLineTarget,
          geodesic: true,
          strokeColor: '#0000FF',
          strokeOpacity: 1.0,
          strokeWeight: 2
        });

        targetPath.setMap(jadyMap);


        console.log(sessionCoordinates);
      } else {
        alert("Can't find Tracking ID. Please check if you misspelled your tracking ID.");
        console.log("[CHILD Data] Can't find tracking ID");
      }
    }


    //New functionality to add Geofence
    var dbGeofence = db.ref('trackingSession/' + inputSessionID + "/geofence");
    dbGeofence.on('value', showGeofence, logError);

    function showGeofence(items) {//GEOFENCE Functionality
      console.log("showGeofence nih"); //contoh ambil sessionID specific: "-L_tDIFtcLQxppYhCw5z"
      console.log(items.val());

      if (items.val() != null) {

        //clear previous array before pushing new stuff//
        geofenceCoordinates = [];

        items.forEach(function (child) {
          //coba push ke array ini
          geofenceCoordinates.push([child.val().latitude, child.val().longitude]); //coba push 
          console.log(geofenceCoordinates);
          addGeofenceOnMap();
        })


        //--Create Polyline for the targetPath--//
        var counterPolyLineGeofence;
        var polyLineTargetGeofence = [];
        //Get all the geofence points
        for (counterPolyLineGeofence = 0; counterPolyLineGeofence < geofenceCoordinates.length; counterPolyLineGeofence++) {
          polyLineTargetGeofence.push({ lat: geofenceCoordinates[counterPolyLineGeofence][0], lng: geofenceCoordinates[counterPolyLineGeofence][1] });
        }

        console.log(polyLineTargetGeofence);

        convexHullJady(counterPolyLineGeofence,polyLineTargetGeofence);

        console.log("diatas itu habis session coorddinates nya geofence");

        console.log(geofenceCoordinates);
      } else {
        alert("Failed to load Geofence Coordinates - Make sure that geofence is created beforehand.");
        console.log("[CHILD Data - GEOFENCE] Failed to load Geofence Coordinates =null");
      }
    }

    //--Functionality to add Finish Point and the geofence for the finish point
    var dbDestinationPoint = db.ref('trackingSession/' + inputSessionID + "/destination");
    dbDestinationPoint.on('value', addDestinationPoint, logError);

    function addDestinationPoint(items) {//Add Destination
      console.log("Destination Points:");
      console.log(items.val());

      if (items.val() != null) {

        //clear previous array before pushing new stuff//
        destinationCoordinates = [];
        destinationCoordinates.push(items.val().latitude);
        destinationCoordinates.push(items.val().longitude);
        destinationCoordinates.push(items.val().radius);
        console.log("destinationCoordinates: ")
        console.log(destinationCoordinates);

        // Add the circle for this city to the map.
        var destinationGeofence = new google.maps.Circle({
          strokeColor: '#71EBFF',
          strokeOpacity: 0.8,
          strokeWeight: 2,
          fillColor: '#90FFFF',
          fillOpacity: 0.35,
          map: jadyMap,
          center: new google.maps.LatLng(destinationCoordinates[0], destinationCoordinates[1]),
          radius: (destinationCoordinates[2])
        });

        //Create marker with initial start line (marker 0)
        marker = new google.maps.Marker({
          position: new google.maps.LatLng(destinationCoordinates[0], destinationCoordinates[1]),
          icon: "images/finish.png",
          map: jadyMap //attach the map onto our map name (our map name is "map" -james)
        });

        //Move Camera
        jadyMap.setZoom(14);
        jadyMap.panTo(new google.maps.LatLng(destinationCoordinates[0], destinationCoordinates[1])); //Pan to the first coordinate

      } else {
        alert("Failed to load Destination Coordinates - Make sure that the destination is created beforehand.");
        console.log("[CHILD Data - Destination] Failed to load Destination Coordinates =null");
      }
    }
    //---

    document.getElementById('mapView').scrollIntoView();

  }
}

//================------------------------Global Variables------------------------================//

//------------------------Update JadyTrack Map------------------------//
function UpdateMapJady() {
  //Add marker dan listener baru saat array di refresh
  var marker, i;

  //Create marker with initial start line (marker 0)
  marker = new google.maps.Marker({
    position: new google.maps.LatLng(sessionCoordinates[0][1], sessionCoordinates[0][2]),
    icon: "images/start.png",
    map: jadyMap //attach the map onto our map name (our map name is "map" -james)
  });

  //Create markers after the initial start line
  for (i = 1; i < sessionCoordinates.length; i++) { //awalnya locations
    marker = new google.maps.Marker({
      position: new google.maps.LatLng(sessionCoordinates[i][1], sessionCoordinates[i][2]),
      icon: "images/greendot.png",
      map: jadyMap //attach the map onto our map name (our map name is "map" -james)
    });

    google.maps.event.addListener(marker, 'click', (function (marker, i) {
      return function () {

        //Convert timestamp to human readable time
        // Create a new JavaScript Date object based on the timestamp
        // multiplied by 1000 so that the argument is in milliseconds, not seconds.
        var date = new Date(sessionCoordinates[i][0]);
        var year = date.getFullYear();
        var month = calendarMonths[date.getMonth()];
        var tanggal = date.getDate();
        var hours = date.getHours();        // Hours part from the timestamp
        var minutes = "0" + date.getMinutes();        // Minutes part from the timestamp
        var seconds = "0" + date.getSeconds();        // Seconds part from the timestamp
        // Will display time in dd/mm/yy (10:30:23) format
        var formattedTime = tanggal + ' ' + month + ' ' + year + ' (' + hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2) + ')';

        infowindow.setContent(formattedTime); //get array value on 0 (name string)

        //---end of convert timestamp
        infowindow.open(jadyMap, marker);
      };
    })(marker, i));
  }

}
//END OF:Update JadyTrack Map--//

//Add Geofence on Map//
function addGeofenceOnMap() {
  //Add marker dan listener baru saat array di refresh
  var marker, i;

  //jadyMap.removeMarkers();
  for (i = 0; i < geofenceCoordinates.length; i++) { //awalnya locations
    marker = new google.maps.Marker({
      position: new google.maps.LatLng(geofenceCoordinates[i][0], geofenceCoordinates[i][1]),
      icon: {
        path: google.maps.SymbolPath.CIRCLE,
      },
      map: jadyMap //attach the map onto our map name (our map name is "map" -james)
    });
    google.maps.event.addListener(marker, 'click', (function (marker, i) {
      return function () {
        infowindow.setContent("Geofence point"); //get array value on 0 (name string)
        infowindow.open(jadyMap, marker);
      };
    })(marker, i));
  }

}
//Add Finish line and the Circular Finish Line Geofence//

//------------------------Update UI------------------------//
function updateLabels() {//update labels for coords target and targetname
  if (manualCheckIn == true) {
    document.getElementById("titleManualCheckIn").innerHTML = "Check In Status: " + manualCheckIn + " (User manually pressed the Check In button)";
  } else {
    document.getElementById("titleManualCheckIn").innerHTML = "Check In Status: " + manualCheckIn;
  }
  document.getElementById("titleInput").innerHTML = "Session ID: " + inputSessionID;
  document.getElementById("titleNumHistory").innerHTML = "Number of Coordinates: " + numHistory;
  document.getElementById("titleTargetId").innerHTML = "Target ID: " + targetId;
  document.getElementById("titleTargetName").innerHTML = "Target Name: " + targetName;

  document.getElementById("titleHasArrived").innerHTML = "Has Arrived: " + hasArrived;
  document.getElementById("titleInsideGeofence").innerHTML = "Inside Geofence: " + statusInGeofence;
  document.getElementById("titleStatusSOS").innerHTML = "Status SOS: " + statusSOS;
  document.getElementById("titleLinkExpired").innerHTML = "Link Expired: " + linkExpired;

}

function updateLabelsAsError() {
  document.getElementById("titleInput").innerHTML = "Session ID: Cant find \"" + inputSessionID + "\"";
  document.getElementById("titleNumHistory").innerHTML = "Number of Coordinates: Cant find \"" + inputSessionID + "\"";
  document.getElementById("titleTargetId").innerHTML = "Target ID: Cant find \"" + inputSessionID + "\"";
  document.getElementById("titleTargetName").innerHTML = "Target Name: Cant find \"" + inputSessionID + "\"";

  document.getElementById("titleHasArrived").innerHTML = "Has Arrived: Cant find \"" + inputSessionID + "\"";
  document.getElementById("titleManualCheckIn").innerHTML = "Check In Status: Cant find \"" + inputSessionID + "\"";
  document.getElementById("titleInsideGeofence").innerHTML = "Inside Geofence: Cant find \"" + inputSessionID + "\"";
  document.getElementById("titleStatusSOS").innerHTML = "Status SOS: Cant find \"" + inputSessionID + "\"";
  document.getElementById("titleLinkExpired").innerHTML = "Link Expired: Cant find \"" + inputSessionID + "\"";
}
//END OF:Update UI--//

//------------------------Convex Hull Polygon Functionality------------------------//
function convexHullJady(counterPolyLineGeofence,polyLineTargetGeofence) {
  var polyHull;
  var convexHull = new ConvexHullGrahamScan();


  //--Geofence Coordinate arraynya dimasukin kedalam object ConvexHullGrahamScan
  polyLineTargetGeofence.forEach(function (item) {
      var marker = new google.maps.Marker({
          position: new google.maps.LatLng(item.lat, item.lng),
          map: map
      });
      convexHull.addPoint(item.lng, item.lat);
  });

  //--if convexHull has at least 1 point then
  if (convexHull.points.length > 0) {

      //get Hull and the nput into hullPoints var
      var hullPoints = convexHull.getHull();

      //Convert to google latlng objects
      hullPoints = hullPoints.map(function (item) {
          return new google.maps.LatLng(item.y, item.x);
      });

      console.log(hullPoints);

      polyHull = new google.maps.Polygon({
          paths: hullPoints,
          strokeColor: '#3B9A43',
          strokeOpacity: 0.8,
          strokeWeight: 2,
          fillColor: '#90EE90',
          fillOpacity: 0.35
      });
      //set the polyhull polygon into the map
      polyHull.setMap(jadyMap);

  }
}