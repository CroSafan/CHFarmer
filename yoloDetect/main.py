from ultralytics import YOLO

model= YOLO('yolov8_custom.pt')

model.predict(source='img1.bmp',show=True,save=True,conf=0.5)