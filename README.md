<h1 align="center">Hyperion-9614/UltimateGoal Build Status</h1>
<p align="center">
<a href="https://travis-ci.com/Alpheron/HyperionFTC"><img alt="Build Status" src="https://travis-ci.com/Hyperion-9614/UltimateGoal.svg?token=mfjmzBAuLfxQsS9xKwRc&branch=master"></a>

<h2>Setup</h2>

* Go to File -> Project Structure
    * Select a version of jdk 8
* Create a new run configuration (Application)
    * Give the configuration the name "Dashboard"
    * Select the Dashboard class in HyperiLibs as the main class
    * Select the UltimateGoal.HyperiLibs module

<h2>How to run (debugging/non-competition)</h2>

* Ensure that the "dashboard.isDebugging" flag in constants.json is true
* Run the Dashboard application
* Using the Android Device File Explorer tab, move constants.json and field.json to
  the RC's data/data/com.qualcomm.ftcrobotcontroller/files/hyperilibs folder
  * If this is the first time, you'll have to make a hyperilibs folder manually in
    data/data/com.qualcomm.ftcrobotcontroller/files 
* Build and run this application on the RC, then select and run an opmode

<h2>Libraries Used</h2>

<u>Apache Commons Math3</u>: org.apache.commons:commons-math3:3.6.1
* Provides advanced math functions, such as matrices and integration

<u>Mariuszgromada’s mXparser</u>: org.mariuszgromada.math:MathParser.org-mXparser:4.4.2
* Provides an easy way to parse and manipulate equations using Strings

<u>org.json</u>: org.json:json:20201115
* JSON parsing and manipulation

<u>underscore</u>: com.github.javadev:underscore:1.60
* Utility library - makes modern programming features available in Java

<u>RevExtensions</u>: org.openftc:rev-extensions-2:1.2
* Allows a programmer to more thoroughly access the capabilities of the FTC REV Expansion Hub, such as LED colors and bulk data reading

<u>CommonsIO</u>: commons-io:commons-io:2.8.0
* Improves input/output functionality

<h2>Current Features</h2>

<b>Spline Trajectory Calculator</b>
* Computes and stores (JSON) a 2D cubic spline curve, essentially solving for the coefficients of 2(n - 1) cubic polynomials, n = # of waypoints, using continuity and a large tridiagonal matrix
* Interpolates to switch the input from the meaningless parametric “T” to distance, more useful for the motion profile
* Resources used
    * https://timodenk.com/blog/cubic-spline-interpolation/
    * http://www.nabla.hr/PC-ParametricEqu1.htm
    * https://people.cs.clemson.edu/~dhouse/courses/405/notes/splines.pdf
    * https://homepage.cs.uiowa.edu/~kearney/pubs/CurvesAndSurfacesArcLength.pdf
    
<b>Motion Profile</b>
* Creates piecewise equations that output the optimal velocity and acceleration at a given distance, applying basic kinematic equations that relate velocity, acceleration and distance
* Each interval represents a segment along the profile’s spline, each the same length
* Resources used
    * http://www2.informatik.uni-freiburg.de/~lau/students/Sprunk2008.pdf
    * https://www.math.usm.edu/lambers/mat169/fall09/lecture32.pdf
    
<b>Localization</b>
* Views the field as an x, y coordinate plane
* Calculates the change of the robot’s position and heading every 10 milliseconds, according to 3 odometry sensors (int values), and adds its it to a manually set initial pose
* Resources used
    * https://github.com/acmerobotics/road-runner/blob/master/doc/pdf/Mobile_Robot_Kinematics_for_FTC.pdf
    
<b>PID</b>
* Calculates an error correction vector (tuned custom Proportional-Integral-Derivative loop) for a robot to apply while following a path
* Resources used
    * https://en.wikipedia.org/wiki/PID_controller
    * https://www.ni.com/en-us/innovations/white-papers/06/pid-theory-explained.html
    
<b>Constants</b>
* Allows values in a multi-level constants.json to be read from and written to using a flat, dot-separated ID string

<b>Dashboard</b>
* Modern, colorful GUI that allows a user to easily edit splines and waypoints, edit constants, view robot telemetry, and view and simulate paths

<b>Simulator</b>
* Feature of the Dashboard
* Visualizes how the robot would follow a spline according to its motion profile, randomly-introduced error, and PID correction

<b>Socket Communication System</b>
* Event-based communication between two sockets (robot controller phone and laptop running the Dashboard)
* Allows a user to see a real-time representation of where the robot thinks it is on the field, according to the localization algorithm

<b>OpenCV Pipeline</b>
* Uses custom OpenCV C++ code to identify the number of orange rings in a stack
