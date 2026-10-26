package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

@TeleOp(name = "13965 Mechanism Test", group = "RoboGoats Tests")
public class RoboGoatsMechanismTest extends LinearOpMode {
    // Enable individually only after the relevant commissioning steps.
    private static final boolean INTAKE_VERIFIED = false;
    private static final boolean SHOOTER_VERIFIED = false;
    private static final boolean FEEDER_CALIBRATED = false;

    private static final double SHOOTER_TPS = 1250; // Connelly target, encoder ticks/sec
    private static final double SHOOTER_MIN_TPS = 1200; // Connelly feed threshold, encoder ticks/sec
    private static final double FEEDER_POWER = 1.0; // Match Connelly continuous feeding power

    @Override
    public void runOpMode() throws InterruptedException {
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

        telemetry.addLine("Stationary test. Uses gamepad 2.");
        telemetry.addLine("RT intake, LT reverse, hold Y shooter + automatic feed, B cancel");
        telemetry.addData("Intake verified", INTAKE_VERIFIED);
        telemetry.addData("Shooter verified", SHOOTER_VERIFIED);
        telemetry.addData("Feeder calibrated", FEEDER_CALIBRATED);
        telemetry.update();
        waitForStart();

        try {
            if (isStopRequested()) return;
            feeder.setPower(0);
            while (opModeIsActive()) {
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
                telemetry.addData("Intake power", intakePower);
                telemetry.addData("Shooter requested / measured", "%.0f / %.0f",
                        enabled ? SHOOTER_TPS : 0, measured);
                telemetry.addData("Shooter ready", ready);
                telemetry.addData("Feeder running", feeding);
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
