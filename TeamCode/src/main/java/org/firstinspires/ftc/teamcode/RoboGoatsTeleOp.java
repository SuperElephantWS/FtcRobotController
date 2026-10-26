/* Copyright (c) 2021 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

@TeleOp(name = "13965 RoboGoats TeleOp", group = "RoboGoats")
/** Combined field-oriented mecanum drive and three-CR-servo mechanism control. */
public class RoboGoatsTeleOp extends LinearOpMode {
    // Set these to the actual Control Hub mounting orientation before driving.
    private static final RevHubOrientationOnRobot.LogoFacingDirection HUB_LOGO_DIRECTION =
            RevHubOrientationOnRobot.LogoFacingDirection.RIGHT;
    private static final RevHubOrientationOnRobot.UsbFacingDirection HUB_USB_DIRECTION =
            RevHubOrientationOnRobot.UsbFacingDirection.FORWARD;

    private static final double STICK_DEADBAND = 0.05;

    // Enable individually only after the relevant commissioning steps.
    private static final boolean INTAKE_VERIFIED = false;
    private static final boolean SHOOTER_VERIFIED = false;
    private static final boolean FEEDER_CALIBRATED = false;

    private static final double SHOOTER_TPS = 1250; // Connelly target, encoder ticks/sec
    private static final double SHOOTER_MIN_TPS = 1200; // Connelly feed threshold, encoder ticks/sec
    private static final double FEEDER_POWER = 1.0; // Match Connelly continuous feeding power

    @Override
    public void runOpMode() throws InterruptedException {
        DcMotor frontLeft = hardwareMap.get(DcMotor.class, "left_front_drive");
        DcMotor frontRight = hardwareMap.get(DcMotor.class, "right_front_drive");
        DcMotor backLeft = hardwareMap.get(DcMotor.class, "left_back_drive");
        DcMotor backRight = hardwareMap.get(DcMotor.class, "right_back_drive");

        // Match RoboGoatsDrive; verify these directions on the assembled robot.
        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.FORWARD);
        DcMotor[] driveMotors = {frontLeft, frontRight, backLeft, backRight};
        for (DcMotor motor : driveMotors) {
            motor.setPower(0);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        DcMotor intake = hardwareMap.get(DcMotor.class, "intake");
        CRServo left = hardwareMap.get(CRServo.class, "left_intake_servo");
        CRServo right = hardwareMap.get(CRServo.class, "right_intake_servo");
        DcMotorEx shooter = hardwareMap.get(DcMotorEx.class, "launcher");
        CRServo feeder = hardwareMap.get(CRServo.class, "windmillServo");
        feeder.setDirection(CRServo.Direction.REVERSE); // Match Connelly windmillServo direction.
        feeder.setPower(0);

        intake.setPower(0);
        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake.setDirection(DcMotor.Direction.FORWARD); // VERIFY
        left.setDirection(CRServo.Direction.FORWARD);    // VERIFY
        right.setDirection(CRServo.Direction.REVERSE);   // VERIFY
        left.setPower(0);
        right.setPower(0);
        shooter.setPower(0);
        shooter.setDirection(DcMotor.Direction.FORWARD); // VERIFY
        shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooter.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER,
                new PIDFCoefficients(40, 0, 0, 12.5));

        IMU imu = hardwareMap.get(IMU.class, "imu");
        if (!imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                HUB_LOGO_DIRECTION, HUB_USB_DIRECTION)))) {
            throw new IllegalStateException("IMU initialization failed");
        }
        boolean previousHeadingReset = false;
        telemetry.addLine("Field-oriented: point robot forward on the field before START");
        telemetry.addLine("Gamepad 1 START: reset field-forward direction");

        telemetry.addLine("13965 RoboGoats TeleOp");
        telemetry.addLine("Gamepad 1: left stick drive/strafe, right stick turn");
        telemetry.addLine("Gamepad 2: mechanism controls below");
        telemetry.addLine("RT intake, LT reverse, hold Y shooter + automatic feed, B cancel");
        telemetry.addData("Intake verified", INTAKE_VERIFIED);
        telemetry.addData("Shooter verified", SHOOTER_VERIFIED);
        telemetry.addData("Feeder calibrated", FEEDER_CALIBRATED);
        telemetry.update();
        try {
            waitForStart();
            if (isStopRequested()) return;
            imu.resetYaw();
            feeder.setPower(0);
            while (opModeIsActive()) {
                // Update every subsystem each pass; no blocking feed delays.
                if (gamepad1.start && !previousHeadingReset) imu.resetYaw();
                previousHeadingReset = gamepad1.start;
                double heading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
                double fieldForward = applyDeadband(-gamepad1.left_stick_y);
                double fieldRight = applyDeadband(gamepad1.left_stick_x);
                // Rotate field translation into the robot's forward/right frame.
                double forward = fieldForward * Math.cos(heading) - fieldRight * Math.sin(heading);
                double rightCommand = fieldForward * Math.sin(heading) + fieldRight * Math.cos(heading);
                double clockwise = applyDeadband(gamepad1.right_stick_x);
                double frontLeftPower = forward + rightCommand + clockwise;
                double frontRightPower = forward - rightCommand - clockwise;
                double backLeftPower = forward - rightCommand + clockwise;
                double backRightPower = forward + rightCommand - clockwise;
                double denominator = Math.max(1.0,
                        Math.max(Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
                                 Math.max(Math.abs(backLeftPower), Math.abs(backRightPower))));
                frontLeftPower = frontLeftPower / denominator;
                frontRightPower = frontRightPower / denominator;
                backLeftPower = backLeftPower / denominator;
                backRightPower = backRightPower / denominator;
                frontLeft.setPower(frontLeftPower);
                frontRight.setPower(frontRightPower);
                backLeft.setPower(backLeftPower);
                backRight.setPower(backRightPower);

                boolean cancel = gamepad2.b;
                double intakePower = 0;
                if (INTAKE_VERIFIED && !cancel) {
                    intakePower = gamepad2.right_trigger - gamepad2.left_trigger;
                }
                intake.setPower(intakePower);
                left.setPower(intakePower);
                right.setPower(intakePower);

                boolean enabled = SHOOTER_VERIFIED && SHOOTER_TPS > 0
                        && gamepad2.y && !cancel;
                if (enabled) shooter.setVelocity(SHOOTER_TPS);
                else shooter.setPower(0);

                double measured = shooter.getVelocity();
                boolean ready = enabled && measured > SHOOTER_MIN_TPS;
                boolean feeding = FEEDER_CALIBRATED && ready;
                feeder.setPower(feeding ? FEEDER_POWER : 0.0);
                telemetry.addData("Field heading (deg)", Math.toDegrees(heading));
                telemetry.addData("FL / FR", "%.2f / %.2f", frontLeftPower, frontRightPower);
                telemetry.addData("BL / BR", "%.2f / %.2f", backLeftPower, backRightPower);
                telemetry.addData("Intake power", intakePower);
                telemetry.addData("Shooter requested / measured", "%.0f / %.0f",
                        enabled ? SHOOTER_TPS : 0, measured);
                telemetry.addData("Shooter ready", ready);
                telemetry.addData("Feeder running", feeding);
                telemetry.update();
                idle();
            }
        } finally {
            for (DcMotor motor : driveMotors) {
                motor.setPower(0);
            }
            intake.setPower(0);
            left.setPower(0);
            right.setPower(0);
            shooter.setPower(0);
            feeder.setPower(0);
        }
    }
    private static double applyDeadband(double value) {
        return Math.abs(value) < STICK_DEADBAND ? 0.0 : value;
    }
}
