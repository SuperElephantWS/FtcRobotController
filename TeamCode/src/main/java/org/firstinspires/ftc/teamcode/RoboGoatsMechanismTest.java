package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp(name = "13965 Mechanism Test", group = "RoboGoats Tests")
public class RoboGoatsMechanismTest extends LinearOpMode {
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
        telemetry.addLine("Stationary test. Uses gamepad 2.");
        telemetry.addLine("RT intake, LT reverse, hold Y shooter, A feed, B cancel");
        telemetry.addData("Intake verified", INTAKE_VERIFIED);
        telemetry.addData("Shooter verified", SHOOTER_VERIFIED);
        telemetry.addData("Feeder calibrated", FEEDER_CALIBRATED);
        telemetry.update();
        waitForStart();

        try {
            if (isStopRequested()) return;
            feeder.setPower(0);
            while (opModeIsActive()) {
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
                telemetry.addData("Intake power", intakePower);
                telemetry.addData("Shooter requested / measured", "%.0f / %.0f",
                        enabled ? SHOOTER_TPS : 0, measured);
                telemetry.addData("Shooter ready", ready);
                telemetry.addData("Feeder", state);
                telemetry.update();
                idle();
            }
        } finally {
            intake.setPower(0);
            left.setPower(0);
            right.setPower(0);
            shooter.setPower(0);
            feeder.setPower(0);
        }
    }
}
