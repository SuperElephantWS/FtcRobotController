package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;

@TeleOp(name = "13965 Servo Calibration", group = "RoboGoats Tests")
public class RoboGoatsServoTest extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        // Test intake_left or intake_right by changing this name.
        CRServo servo = hardwareMap.get(CRServo.class, "feeder");
        servo.setDirection(CRServo.Direction.FORWARD); // VERIFY
        servo.setPower(0);
        telemetry.addLine("Hold A: +0.15; hold B: -0.15; release: stop.");
        telemetry.update();
        waitForStart();
        try {
            while (opModeIsActive()) {
                double power = gamepad1.b ? -0.15 : (gamepad1.a ? 0.15 : 0.0);
                servo.setPower(power);
                telemetry.addData("Commanded power", power);
                telemetry.update();
                idle();
            }
        } finally {
            servo.setPower(0);
        }
    }
}
