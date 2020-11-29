import cv2
import numpy as np


class ColorTuner:

    def __init__(self, path, downScale):
        self.main(path, downScale)

    def nothing(self, x):
        pass

    def main(self, path, downScale):
        # Load image
        unsizedImage = cv2.imread(path)
        image = cv2.resize(unsizedImage,
                           (int(unsizedImage.shape[1] / downScale),
                            (int(unsizedImage.shape[0] / downScale))))
        # Create a window
        cv2.namedWindow('image')

        # Create trackbars for color change
        # Hue is from 0-179 for Opencv
        cv2.createTrackbar('YMin', 'image', 0, 255, self.nothing)
        cv2.createTrackbar('CrMin', 'image', 0, 255, self.nothing)
        cv2.createTrackbar('CbMin', 'image', 0, 255, self.nothing)
        cv2.createTrackbar('YMax', 'image', 0, 255, self.nothing)
        cv2.createTrackbar('CrMax', 'image', 0, 255, self.nothing)
        cv2.createTrackbar('CbMax', 'image', 0, 255, self.nothing)

        # Set default value for Max YCrCb trackbars
        cv2.setTrackbarPos('YMax', 'image', 255)
        cv2.setTrackbarPos('CrMax', 'image', 255)
        cv2.setTrackbarPos('CbMax', 'image', 255)

        # Initialize YCrCb min/max values
        YMin = CrMin = CbMin = YMax = CrMax = CbMax = 0
        pYMin = pCrMin = pCbMin = pYMax = pCrMax = pCbMax = 0

        while 1:
            # Get current positions of all trackbars
            YMin = cv2.getTrackbarPos('YMin', 'image')
            CrMin = cv2.getTrackbarPos('CrMin', 'image')
            CbMin = cv2.getTrackbarPos('CbMin', 'image')
            YMax = cv2.getTrackbarPos('YMax', 'image')
            CrMax = cv2.getTrackbarPos('CrMax', 'image')
            CbMax = cv2.getTrackbarPos('CbMax', 'image')

            # Set minimum and maximum YCrCb values to display
            lower = np.array([YMin, CrMin, CbMin])
            upper = np.array([YMax, CrMax, CbMax])

            # Convert to YCrCb format and color threshold
            YCrCb = cv2.cvtColor(image, cv2.COLOR_BGR2YCR_CB)
            mask = cv2.inRange(YCrCb, lower, upper)
            result = cv2.bitwise_and(image, image, mask=mask)

            # Print if there is a change in YCrCb value
            if ((pYMin != YMin) | (pCrMin != CrMin) | (pCbMin != CbMin) | (pYMax != YMax) | (
                    pCrMax != CrMax) | (
                    pCbMax != CbMax)):
                print(
                    "(YMin = %d , CrMin = %d, CbMin = %d), (YMax = %d , CrMax = %d, CbMax = %d)" % (
                        YMin, CrMin, CbMin, YMax, CrMax, CbMax))
                pYMin = YMin
                pCrMin = CrMin
                pCbMin = CbMin
                pYMax = YMax
                pCrMax = CrMax
                pCbMax = CbMax

            # Display result image
            cv2.imshow('image', result)
            if cv2.waitKey(10) & 0xFF == ord('q'):
                break

        cv2.destroyAllWindows()


if __name__ == "__main__":
    # Tune this as good as possible
    # Rings will be pretty easy to pick up
    # Make sure to not touch Y since that is lighting and we want as little light influence as possible
    # Make sure not to crop or cut out rings with filtering
    # Make sure this is p good cause this is the final filter
    # Any false positive or improperly tuned objects will be picked up and may mess up readings
    colorTuner = ColorTuner('[INSERT PATH HERE]', 5.5)
