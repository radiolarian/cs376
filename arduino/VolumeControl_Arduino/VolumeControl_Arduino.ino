#include <Servo.h>

#define BAUD_RATE 9600
#define MICROPHONE_PIN 14
#define MOTOR_PIN 23
#define MOTOR_MIN_PULSE_WIDTH 1000
#define MOTOR_MAX_PULSE_WIDTH 2000
#define MIN_POS 10
#define MAX_POS 170
#define MIN_MICROPHONE_VALUE 450
#define MAX_MICROPHONE_VALUE 1200
#define MOVING_AVERAGE_ALPHA .2

double movingAverage;
Servo motor;

void setup() {
  // put your setup code here, to run once:
  Serial.begin(BAUD_RATE);

  motor.attach(MOTOR_PIN, MOTOR_MIN_PULSE_WIDTH, MOTOR_MAX_PULSE_WIDTH);
  movingAverage = analogRead(MICROPHONE_PIN);
}

void loop() {
  // put your main code here, to run repeatedly:
  int newVolume = analogRead(MICROPHONE_PIN);
  movingAverage = (MOVING_AVERAGE_ALPHA * newVolume) + ((1 - MOVING_AVERAGE_ALPHA) * movingAverage);

  int pos = map(movingAverage, MIN_MICROPHONE_VALUE, MAX_MICROPHONE_VALUE, MIN_POS, MAX_POS);
  motor.write(pos);
}
