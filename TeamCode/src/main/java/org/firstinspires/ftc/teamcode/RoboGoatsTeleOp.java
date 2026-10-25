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

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp(name = "13965 RoboGoats TeleOp", group = "RoboGoats")
/** Combined robot-relative mecanum drive and three-CR-servo mechanism control. */
public class RoboGoatsTeleOp extends LinearOpMode {
    private static final double DRIVE_POWER_SCALE = 0.30;
    private static final double SLOW_DRIVE_POWER_SCALE = 0.15;
    private static final double STICK_DEADBAND = 0.05;

    // Enable individually only after the relevant commissioning steps.
    private static final boolean INTAKE_VERIFIED = false;
    private static final boolean SHOOTER_VERIFIED = false;
    private static final boolean FEEDER_CALIBRATED = false;

    private static final double SHOOTER_TPS = 0;       // MEASURE, must be > 0
    private static final double SPEED_TOLERANCE_TPS = 0; // MEASURE, must be > 0
    private static final double FEEDER_POWER = 0.15;  // CALIBRATE power and direction
    private static final double FEED_SECONDS = 0.20;  // CALIBRATE one-piece pulse
    private static final double SETTLE_SECONDS = 0.25;// CALIBRATE stopped interval
    private enum FeedState { REST, FEED, SETTLE }

    @Override
    public void runOpMode() throws InterruptedException {
        DcMotor frontLeft = hardwareMap.get(DcMotor.class, "front_left_drive");
        DcMotor frontRight = hardwareMap.get(DcMotor.class, "front_right_drive");
        DcMotor backLeft = hardwareMap.get(DcMotor.class, "back_left_drive");
        DcMotor backRight = hardwareMap.get(DcMotor.class, "back_right_drive");

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

        DcMotor intake = hardwareMap.get(DcMotor.class, "intake_motor");
        CRServo left = hardwareMap.get(CRServo.class, "intake_left");
        CRServo right = hardwareMap.get(CRServo.class, "intake_right");
        DcMotorEx shooter = hardwareMap.get(DcMotorEx.class, "shooter_motor");
        CRServo feeder = hardwareMap.get(CRServo.class, "feeder");
        feeder.setDirection(CRServo.Direction.FORWARD); // VERIFY positive feeds
        feeder.setPower(0);

        intake.setPower(0);
        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        intake.setDirection(DcMotor.Direction.FORWARD); // VERIFY
        left.setDirection(CRServo.Direction.FORWARD);    // VERIFY
        right.setDirection(CRServo.Direction.REVERSE);   // VERIFY
        left.setPower(0);
        right.setPower(0);
        shooter.setPower(0);
        shooter.setDirection(DcMotor.Direction.FORWARD); // VERIFY
        shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        FeedState state = FeedState.REST;
        ElapsedTime clock = new ElapsedTime();
        double stateStarted = 0;
        double withinSpeedSince = -1;
        boolean previousFeedButton = false;
        telemetry.addLine("13965 RoboGoats TeleOp");
        telemetry.addLine("Gamepad 1: left stick drive/strafe, right stick turn, RB slow");
        telemetry.addLine("Gamepad 2: mechanism controls below");
        telemetry.addLine("RT intake, LT reverse, hold Y shooter, A feed, B cancel");
        telemetry.addData("Intake verified", INTAKE_VERIFIED);
        telemetry.addData("Shooter verified", SHOOTER_VERIFIED);
        telemetry.addData("Feeder calibrated", FEEDER_CALIBRATED);
        telemetry.update();
        try {
            waitForStart();
            if (isStopRequested()) return;
            feeder.setPower(0);
            while (opModeIsActive()) {
                // Update every subsystem each pass; no blocking feed delays.
                double forward = applyDeadband(-gamepad1.left_stick_y);
                double rightCommand = applyDeadband(gamepad1.left_stick_x);
                double clockwise = applyDeadband(gamepad1.right_stick_x);
                double frontLeftPower = forward + rightCommand + clockwise;
                double frontRightPower = forward - rightCommand - clockwise;
                double backLeftPower = forward - rightCommand + clockwise;
                double backRightPower = forward + rightCommand - clockwise;
                double denominator = Math.max(1.0,
                        Math.max(Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
                                 Math.max(Math.abs(backLeftPower), Math.abs(backRightPower))));
                double driveScale = gamepad1.right_bumper
                        ? SLOW_DRIVE_POWER_SCALE : DRIVE_POWER_SCALE;
                frontLeftPower = frontLeftPower / denominator * driveScale;
                frontRightPower = frontRightPower / denominator * driveScale;
                backLeftPower = backLeftPower / denominator * driveScale;
                backRightPower = backRightPower / denominator * driveScale;
                frontLeft.setPower(frontLeftPower);
                frontRight.setPower(frontRightPower);
                backLeft.setPower(backLeftPower);
                backRight.setPower(backRightPower);

                double now = clock.seconds();
                boolean cancel = gamepad2.b;
                double intakePower = 0;
                if (INTAKE_VERIFIED && !cancel) {
                    if (gamepad2.left_trigger > 0.1) intakePower = -0.25;
                    else if (gamepad2.right_trigger > 0.1) intakePower = 0.40;
                }
                intake.setPower(intakePower);
                left.setPower(intakePower);
                right.setPower(intakePower);

                boolean enabled = SHOOTER_VERIFIED && SHOOTER_TPS > 0
                        && gamepad2.y && !cancel;
                if (enabled) shooter.setVelocity(SHOOTER_TPS);
                else shooter.setPower(0);

                double measured = shooter.getVelocity();
                boolean inBand = enabled && SPEED_TOLERANCE_TPS > 0
                        && Math.abs(measured - SHOOTER_TPS) <= SPEED_TOLERANCE_TPS;
                if (!inBand) withinSpeedSince = -1;
                else if (withinSpeedSince < 0) withinSpeedSince = now;
                boolean ready = inBand && now - withinSpeedSince >= 0.25;
                boolean feedEdge = gamepad2.a && !previousFeedButton;
                previousFeedButton = gamepad2.a;

                if (!enabled || cancel || !FEEDER_CALIBRATED) {
                    state = FeedState.REST;
                } else if (state == FeedState.REST && feedEdge && ready) {
                    state = FeedState.FEED;
                    stateStarted = now;
                    withinSpeedSince = -1; // Require recovery before another feed.
                } else if (state == FeedState.FEED && now - stateStarted >= FEED_SECONDS) {
                    state = FeedState.SETTLE;
                    stateStarted = now;
                } else if (state == FeedState.SETTLE && now - stateStarted >= SETTLE_SECONDS) {
                    state = FeedState.REST;
                }

                feeder.setPower(FEEDER_CALIBRATED && state == FeedState.FEED
                        ? FEEDER_POWER : 0.0);
                telemetry.addData("Drive scale", driveScale);
                telemetry.addData("FL / FR", "%.2f / %.2f", frontLeftPower, frontRightPower);
                telemetry.addData("BL / BR", "%.2f / %.2f", backLeftPower, backRightPower);
                telemetry.addData("Intake power", intakePower);
                telemetry.addData("Shooter requested / measured", "%.0f / %.0f",
                        enabled ? SHOOTER_TPS : 0, measured);
                telemetry.addData("Shooter ready", ready);
                telemetry.addData("Feeder", state);
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
