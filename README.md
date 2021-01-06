<h1 align="center">Hyperion-9614/UltimateGoal Build Status</h1>
<p align="center">
<a href="https://travis-ci.com/Alpheron/HyperionFTC"><img alt="Build Status" src="https://travis-ci.com/Hyperion-9614/UltimateGoal.svg?token=mfjmzBAuLfxQsS9xKwRc&branch=master"></a>

Setup
* Go to File -> Project Structure
    * Select a version of jdk 8
* Create a new run configuration (Application)
    * Give the configuration the name "Dashboard"
    * Select the Dashboard class in HyperiLibs as the main class
    * Select the UltimateGoal.HyperiLibs module

How to run (debugging/non-competition)
* Ensure that the "dashboard.isDebugging" flag in constants.json is true
* Run the Dashboard application
* Using the Android Device File Explorer tab, move constants.json and field.json to
  the RC's data/data/com.qualcomm.ftcrobotcontroller/files/hyperilibs folder
  * If this is the first time, you'll have to make a hyperilibs folder manually in
    data/data/com.qualcomm.ftcrobotcontroller/files 
* Build and run this application on the RC, then select and run an opmode