package org.firstinspires.ftc.teamcode.modules

import org.openftc.easyopencv.OpenCvPipeline


abstract class CvPipeline : OpenCvPipeline() {

    private val stoneRowMaxWidth = 6
    var skystoneDetected = false
    var detectedSkystonePosition = -1
    val width = 640
    val height = 480


    fun getSkystonePositions(leftMostPosition: Int): Array<Int> {
        val firstSkystonePosition = detectedSkystonePosition + leftMostPosition
        var secondSkystonePosition: Int

        if (firstSkystonePosition >= stoneRowMaxWidth / 2) {
            secondSkystonePosition = firstSkystonePosition - 3
        } else {
            secondSkystonePosition = firstSkystonePosition + 3
        }

        val skystonePositions = arrayOf(firstSkystonePosition, secondSkystonePosition)
        skystonePositions.sort()

        skystoneDetected = true

        return skystonePositions
    }
}