from ultralytics import YOLO
import cv2
import numpy as np
from ctypes import windll
import win32gui
import win32ui
import time
import pyautogui


# model= YOLO('yolov8_custom.pt')

# model.predict(source='img1.bmp',show=True,save=True,conf=0.5)
offset_x = 0
offset_y = 0

def capture_win_alt(window_name: str):
    # Adapted from https://stackoverflow.com/questions/19695214/screenshot-of-inactive-window-printwindow-win32gui

    windll.user32.SetProcessDPIAware()
    hwnd = win32gui.FindWindow(None, window_name)

    left, top, right, bottom = win32gui.GetClientRect(hwnd)
    w = right - left
    h = bottom - top

    hwnd_dc = win32gui.GetWindowDC(hwnd)
    mfc_dc = win32ui.CreateDCFromHandle(hwnd_dc)
    save_dc = mfc_dc.CreateCompatibleDC()
    bitmap = win32ui.CreateBitmap()
    bitmap.CreateCompatibleBitmap(mfc_dc, w, h)
    save_dc.SelectObject(bitmap)


    window_rect = win32gui.GetWindowRect(hwnd)

    offset_x=window_rect[0]
    offset_y=window_rect[1]


    # If Special K is running, this number is 3. If not, 1
    result = windll.user32.PrintWindow(hwnd, save_dc.GetSafeHdc(), 3)

    bmpinfo = bitmap.GetInfo()
    bmpstr = bitmap.GetBitmapBits(True)

    img = np.frombuffer(bmpstr, dtype=np.uint8).reshape(
        (bmpinfo["bmHeight"], bmpinfo["bmWidth"], 4)
    )
    img = np.ascontiguousarray(img)[
        ..., :-1
    ]  # make image C_CONTIGUOUS and drop alpha channel

    if not result:  # result should be 1
        win32gui.DeleteObject(bitmap.GetHandle())
        save_dc.DeleteDC()
        mfc_dc.DeleteDC()
        win32gui.ReleaseDC(hwnd, hwnd_dc)
        raise RuntimeError(f"Unable to acquire screenshot! Result: {result}")

    return img,offset_x,offset_y


def results1(img, results,offset_x,offset_y):
    for result in results:
        for box in result.boxes:
            left, top, right, bottom = np.array(box.xyxy.cpu(), dtype=np.int_).squeeze()
            width = right - left
            height = bottom - top
            center = (left + int((right - left) / 2), top + int((bottom - top) / 2))
            label = results[0].names[int(box.cls)]
            confidence = float(box.conf.cpu())

            cv2.rectangle(img, (left, top), (right, bottom), (255, 0, 0), 2)

            cv2.putText(
                img,
                label,
                (left, bottom + 20),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.6,
                (0, 0, 255),
                1,
                cv2.LINE_AA,
            )

            # Move the mouse to the specified coordinates
            pyautogui.moveTo(results[0].boxes.xyxy.tolist()[0][0]+offset_x, results[0].boxes.xyxy.tolist()[0][1]+offset_y, duration=1)

            # Pause for a short time (you can adjust this based on your needs)
            time.sleep(0.5)

            # Simulate a mouse click (left button)
            pyautogui.click()



    cv2.imshow("Filtered Frame", img)
    # time.sleep(0.5)
    # cv2.waitKey(0)


def main():
    WINDOW_NAME = "Clicker Heroes"
    counter = 0
    results = []
    model = YOLO("yolov8_custom.pt")

    while cv2.waitKey(1) != ord("q"):
        screenshot,offset_x,offset_y = capture_win_alt(WINDOW_NAME)
        input = screenshot.copy()

        if counter == 250:
            results = model.predict(input, show=False, save=False, conf=0.5)
            counter = 0
        else:
            results=[]

            

        results1(np.array(screenshot), results,offset_x+40,offset_y+75)
        counter = counter + 1


if __name__ == "__main__":
    main()
