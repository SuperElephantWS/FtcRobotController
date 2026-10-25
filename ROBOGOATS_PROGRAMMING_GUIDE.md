# RoboGoats 13965: robot programming guide

Team: **13965 — RoboGoats**  
Written: **October 25, 2026**  
Project: FTC Robot Controller, SDK **12.0.0**, Java with Android Studio

This guide starts with the robot you have now and adds capabilities in stages. Complete each stage's acceptance checks before moving on. Keep a working copy of each successful program so that a new mechanism never prevents you from testing the drivetrain.

The Java programs below are examples to create in `TeamCode`; writing this guide does not install them on the robot. Values marked **MEASURE**, **CALIBRATE**, or **VERIFY** require measurements on your robot. They are not goBILDA StarterBot factory settings.

Validation: the five complete Java examples were compiled against the locally cached FTC SDK 12.0.0 and Android API classes when this guide was created. Local reference links were checked. The examples have not been deployed or tested on your robot; the smaller fragments and algorithm sketches are identified separately.

## Contents

1. [Hardware inventory and decisions](#1-hardware-inventory-and-decisions)
2. [Set up the computer and robot](#2-set-up-the-computer-and-robot)
3. [Create the hardware configuration](#3-create-the-hardware-configuration)
4. [First movement: mecanum drive](#4-first-movement-mecanum-drive)
5. [Calibrate servos](#5-calibrate-servos)
6. [Add intake, shooter, and feeder](#6-add-intake-shooter-and-feeder)
7. [Combine driving and mechanisms](#7-combine-driving-and-mechanisms)
8. [Add the two-stage linear slide](#8-add-the-two-stage-linear-slide)
9. [Add Pinpoint and two odometry pods](#9-add-pinpoint-and-two-odometry-pods)
10. [Add the front Limelight](#10-add-the-front-limelight)
11. [Add the slide-mounted and AprilTag Limelights](#11-add-the-slide-mounted-and-apriltag-limelights)
12. [Build autonomous movement](#12-build-autonomous-movement)
13. [Troubleshooting](#13-troubleshooting)
14. [Team calibration record and milestones](#14-team-calibration-record-and-milestones)
15. [References](#15-references)

## 1. Hardware inventory and decisions

### 1.1 What you have now

| Part | Quantity | First programming goal |
|---|---:|---|
| Mecanum drive motor and wheel | 4 | Forward, backward, strafe, turn |
| Intake motor | 1 | Hold to collect; hold another button to reverse |
| Intake side servo | 2 | Confirm whether each rotates continuously or moves to a position |
| Shooter motor | 1 | Measure speed using its encoder; control a target speed |
| Indexing/feeder servo | 1 | One calibrated feed cycle per button press |

You have **six DC motors now**. The slide will bring the total to **seven**. A REV Control Hub has four DC motor ports. The wiring plan here assumes **one Control Hub and one Expansion Hub**, providing eight motor ports. If your kit uses a different extra motor controller, adapt the hardware mapping and speed-control approach to that controller before using the mechanism examples. See [REV Control Hub specifications](https://www.revrobotics.com/rev-31-1595/).

### 1.2 Planned hardware

| Future part | Software responsibility |
|---|---|
| One motor driving a two-stage slide | Homing, encoder position, travel limits, timeout, gravity support |
| Front game-piece Limelight | Detect pieces ahead; later provide driver alignment assistance |
| Slide-mounted Limelight looking down toward rear/sides | Detect pieces with geometry that changes as the slide moves |
| Upward-facing AprilTag Limelight | Detect selected tags; aim; optionally estimate pose using stationary known tags |
| Two goBILDA odometry pods | Measure forward and lateral travel independently of drive wheels |
| goBILDA Pinpoint | Combine pod motion and its internal IMU into robot position and heading |

This guide assumes **Limelight 3A** for its wiring and Java examples. Confirm the exact camera models before buying cables or power equipment. Other Limelight models have different power requirements and connections.

### 1.3 Resolve these details before their stage

Record the following in section 14 as you learn them. You can complete the driving stage before resolving the mechanism details.

- Exact drive, intake, shooter, and slide motor model numbers and gear ratios.
- Whether the intake side servos are **continuous rotation** or **positional**. Some programmable goBILDA servos can be set to different modes; inspect their actual programmed mode.
- Whether the feeder is a positional gate/pusher or a continuous-rotation device. The feeder example assumes **positional**.
- Shooter transmission ratio and motor encoder counts per output-shaft revolution.
- Slide rigging arrangement, usable carriage travel, and travel per motor revolution.
- Which odometry pods you will use: Swingarm, 4-Bar, or another model.
- Exact camera models, firmware, and whether your installed FTC system reliably supports all three together.

### 1.4 Programming order

Follow this order:

1. Drive only.
2. Test each servo separately.
3. Intake motor and side servos.
4. Shooter motor with feeder disconnected.
5. Feeder with shooter-speed interlock.
6. Combined driver control.
7. Slide alone, then slide integration.
8. Pinpoint telemetry and calibration.
9. One Limelight at a time.
10. Camera coexistence and driver assistance.
11. Short autonomous routines, then scoring sequences.

## 2. Set up the computer and robot

### 2.1 Understand where code runs

- **Computer:** Android Studio edits Java, builds the app, and installs it.
- **Control Hub:** runs your Robot Controller app and the selected robot program.
- **Driver Station/Driver Hub:** connects to the robot, receives gamepad inputs, selects the program, and sends INIT/START/STOP.
- **Expansion Hub:** adds hardware ports; you do not install a separate Java app on it.
- **Limelight:** runs its configured vision pipeline; your robot Java code reads the results.

An **OpMode** is one selectable robot program. `@TeleOp` makes it appear in the driver-controlled list; `@Autonomous` makes it appear in the autonomous list. `@Disabled` hides it.

### 2.2 Open this project

1. Install the Android Studio version required by this repository's [README](README.md). This checkout specifies **Android Studio Narwhal 3 Feature Drop or later**.
2. Open the repository root, the folder containing `settings.gradle` and `TeamCode`.
3. Let Android Studio complete Gradle sync and any required Android SDK installation.
4. Use the Java/Gradle JDK supported by the project's Android Gradle plugin; Android Studio's bundled compatible JDK is the usual starting point.
5. Resolve sync errors before writing robot code. An editor without errors is not proof that the app built.
6. Keep the FTC dependency versions together. This checkout uses `12.0.0` in `build.dependencies.gradle`.

### 2.3 Prepare the robot

1. Charge the robot battery and Driver Station.
2. With robot power off, check motor, encoder, servo, hub power, and RS485 wiring.
3. Power both hubs according to their manuals. The RS485 connection carries communication; the Expansion Hub also needs battery power.
4. Turn the robot on and connect the Driver Station to its Control Hub network.
5. Verify compatible Robot Controller and Driver Station app versions. If you update the installed controller app separately, remember that deploying from Android Studio installs the app built from this checkout again.
6. Connect a gamepad to the Driver Station. Assign it to User 1 using the Driver Station's gamepad assignment procedure. On a typical Logitech controller this is Start+A; User 2 is Start+B.
7. Check that each assigned gamepad indicator responds to input.
8. Identify the robot's main power switch and Driver Station STOP control before testing motion.

### 2.4 Create your code in the right directory

All Java files you create in this guide belong here:

```text
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
```

Every complete Java example below starts with:

```java
package org.firstinspires.ftc.teamcode;
```

In Android Studio, expand **TeamCode → java → org.firstinspires.ftc.teamcode**, right-click that package, and choose **New → Java Class**. Use the exact class name from the example. Replace the generated contents with the entire example. A public class named `RoboGoatsDrive` must be in `RoboGoatsDrive.java`.

Keep the SDK samples as references. Copy samples into `TeamCode` before changing them, as described in [FTC's OpMode tutorial](https://ftc-docs.firstinspires.org/en/latest/programming_resources/tutorial_specific/android_studio/creating_op_modes/Creating-and-Running-an-Op-Mode-%28Android-Studio%29.html).

### 2.5 Build and deploy: repeat after each code change

1. Power the robot from its battery.
2. Connect a **data-capable** USB cable from the computer to the Control Hub's **USB-C** port. This port is different from the USB-A host ports used for peripherals.
3. If prompted, authorize USB debugging on the controller. Use REV's setup documentation if Android Studio cannot see the device.
4. In Android Studio, select the **TeamCode** Android app run configuration and the **Control Hub** target device.
5. Click **Run ▶**. Wait for a successful build and installation.
6. Allow the Robot Controller app and Driver Station connection to recover after installation.
7. On the Driver Station, select your saved hardware configuration if needed.
8. Select the intended OpMode under TeleOp.
9. Press **INIT**. Read telemetry and resolve any hardware-name error.
10. Press **START** only after the test area and mechanism are ready.
11. Test, press **STOP**, and record the result.

Installing from Android Studio updates the Robot Controller app on the robot. Merely saving a file on the computer does not update the robot.

## 3. Create the hardware configuration

### 3.1 Suggested port map

This is a proposed map, not a statement about your existing wiring. You may use other ports if the configuration names match the code. Label both ends of each wire.

| Hub | Port type | Port | Configuration name | Stage |
|---|---|---:|---|---|
| Control Hub | Motor | 0 | `front_left_drive` | Drive |
| Control Hub | Motor | 1 | `front_right_drive` | Drive |
| Control Hub | Motor | 2 | `back_left_drive` | Drive |
| Control Hub | Motor | 3 | `back_right_drive` | Drive |
| Expansion Hub | Motor | 0 | `intake_motor` | Intake |
| Expansion Hub | Motor + matching encoder | 1 | `shooter_motor` | Shooter |
| Expansion Hub | Motor + matching encoder | 2 | `slide_motor` | Future slide |
| Expansion Hub | Motor | 3 | Reserved | Future |
| Expansion Hub | Servo | 0 | `intake_left` | Intake |
| Expansion Hub | Servo | 2 | `intake_right` | Intake |
| Expansion Hub | Servo | 4 | `feeder` | Feeder |
| Control Hub | I²C | 1, suggested | `pinpoint` | Future odometry |
| Expansion Hub | Digital input | Record actual channel | `slide_bottom` | Future slide home sensor |
| Control Hub USB host interface | Limelight | Device-dependent | `ll_front` | Future vision |
| Control Hub USB host interface | Limelight | Device-dependent | `ll_slide` | Future vision |
| Control Hub USB host interface | Limelight | Device-dependent | `ll_tags` | Future vision |

The bottom slide sensor is an additional recommended component, not part of your stated inventory. The slide procedure below uses a compatible touch/limit sensor configured as `TouchSensor`. A raw switch configured as `DigitalChannel` needs different code and polarity handling.

Servo ports 0, 2, and 4 put the three servos on different port pairs. These pairs share current limits, and there is also a total shared limit; spreading them out does not remove those limits. Check the actual servo load and supply ratings. See [FTC hub tips](https://ftc-docs.firstinspires.org/en/latest/tech_tips/tech-tips/tech-tip-hub-tips/tech-tip-hub-tips.html).

### 3.2 Configure drive hardware first

1. Open **Configure Robot** from the Driver Station menu; menu wording may vary by app version.
2. Create a configuration and scan the connected hardware.
3. Open the Control Hub's motor configuration.
4. For each of the four drive motor ports, choose the motor type matching the motor's exact model and gear ratio.
5. Enter the four drive names from the table exactly, including underscores.
6. Save as **`RoboGoats_13965`** and make it active.
7. Add other devices to this configuration when you reach their stage. Do not add hardware lookups to the drive-only program for devices you have not installed.
8. After adding an Expansion Hub, scan/configure that hub too. Distinguish the two hubs by their serial numbers and physical labels.

These are different names: `front_left_drive`, `Front_Left_Drive`, and `front left drive`. A `hardwareMap.get(...)` lookup must match the configuration name and device type.

## 4. First movement: mecanum drive

### 4.1 Inspect wheel arrangement

1. Choose and label the robot's front.
2. Confirm the mecanum rollers form the usual **X pattern when viewed from above**, following the wheel assembly instructions.
3. Label each motor FL, FR, BL, or BR.
4. Support the chassis securely with all four wheels free to turn for the first test.

### 4.2 Create `RoboGoatsDrive.java`

This is a complete drive-only program. It includes individual wheel tests so you can separate port-name errors from direction errors. It requires only the four drive motors.

```java
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

@TeleOp(name = "13965 RoboGoats Drive", group = "RoboGoats")
public class RoboGoatsDrive extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        DcMotor fl = hardwareMap.get(DcMotor.class, "front_left_drive");
        DcMotor fr = hardwareMap.get(DcMotor.class, "front_right_drive");
        DcMotor bl = hardwareMap.get(DcMotor.class, "back_left_drive");
        DcMotor br = hardwareMap.get(DcMotor.class, "back_right_drive");

        // Starting assumptions: VERIFY individually on your robot.
        fl.setDirection(DcMotor.Direction.REVERSE);
        bl.setDirection(DcMotor.Direction.REVERSE);
        fr.setDirection(DcMotor.Direction.FORWARD);
        br.setDirection(DcMotor.Direction.FORWARD);

        for (DcMotor motor : new DcMotor[]{fl, fr, bl, br}) {
            motor.setPower(0);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        telemetry.addLine("RoboGoats 13965: drive ready");
        telemetry.addLine("Hold LB for wheel tests: X=FL Y=FR A=BL B=BR");
        telemetry.update();
        waitForStart();

        try {
            while (opModeIsActive()) {
                double pfl, pfr, pbl, pbr;
                if (gamepad1.left_bumper) {
                    // Bench test: 15% power; sticks are ignored in this mode.
                    pfl = gamepad1.x ? 0.15 : 0;
                    pfr = gamepad1.y ? 0.15 : 0;
                    pbl = gamepad1.a ? 0.15 : 0;
                    pbr = gamepad1.b ? 0.15 : 0;
                } else {
                    double forward = -gamepad1.left_stick_y;
                    double right = gamepad1.left_stick_x;
                    double clockwise = gamepad1.right_stick_x;
                    if (Math.abs(forward) < 0.05) forward = 0;
                    if (Math.abs(right) < 0.05) right = 0;
                    if (Math.abs(clockwise) < 0.05) clockwise = 0;

                    pfl = forward + right + clockwise;
                    pfr = forward - right - clockwise;
                    pbl = forward - right + clockwise;
                    pbr = forward + right - clockwise;
                    double denominator = Math.max(1.0,
                            Math.max(Math.max(Math.abs(pfl), Math.abs(pfr)),
                                     Math.max(Math.abs(pbl), Math.abs(pbr))));
                    double scale = gamepad1.right_bumper ? 0.15 : 0.30;
                    pfl = pfl / denominator * scale;
                    pfr = pfr / denominator * scale;
                    pbl = pbl / denominator * scale;
                    pbr = pbr / denominator * scale;
                }
                fl.setPower(pfl);
                fr.setPower(pfr);
                bl.setPower(pbl);
                br.setPower(pbr);
                telemetry.addData("Mode", gamepad1.left_bumper ? "WHEEL TEST" : "DRIVE");
                telemetry.addData("FL / FR", "%.2f / %.2f", pfl, pfr);
                telemetry.addData("BL / BR", "%.2f / %.2f", pbl, pbr);
                telemetry.update();
                idle();
            }
        } finally {
            fl.setPower(0);
            fr.setPower(0);
            bl.setPower(0);
            br.setPower(0);
        }
    }
}
```

The mixing logic corresponds to the SDK's [BasicOmniOpMode_Linear](FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/BasicOmniOpMode_Linear.java). This is **robot-relative** drive: pushing forward always means toward the robot's labeled front.

### 4.3 Test one wheel at a time

1. Deploy the app and select **13965 RoboGoats Drive**.
2. INIT, inspect telemetry, then START with sticks centered.
3. Hold gamepad 1 **left bumper + X**. Only the front-left wheel should move.
4. Release X; that wheel must stop.
5. Repeat with **Y → front right**, **A → back left**, **B → back right**.
6. If the wrong wheel moves, fix the configuration name-to-port mapping. Do not use a direction reversal to fix a wrong motor mapping.
7. For each wheel, positive power must produce the wheel rotation that would drive that corner forward. If needed, change that motor's `setDirection` between FORWARD and REVERSE.
8. STOP and redeploy after changes. Repeat all four checks.

### 4.4 Test the robot on the floor

1. STOP before lowering the robot.
2. Put it on a clear floor and restart the program.
3. Release the left bumper so wheel-test mode is off.
4. Push the left stick forward gently: robot moves forward.
5. Pull it back: robot moves backward.
6. Push left stick right: robot strafes right without deliberate turning.
7. Push left stick left: robot strafes left.
8. Push right stick right: robot turns clockwise when viewed from above.
9. Try diagonal movement, then translation and turning together.
10. Release the sticks: all wheel powers return to zero.
11. Press STOP while moving slowly: motion must cease.

If forward works but strafe fails, recheck wheel placement and individual wheel directions. Do not randomly change the mixer signs.

### 4.5 Acceptance checks

- [ ] Correct wheel responds to each bench-test button.
- [ ] Forward/backward are correct.
- [ ] Left/right strafe are correct.
- [ ] Clockwise/counterclockwise turns are correct.
- [ ] Releasing sticks stops movement.
- [ ] STOP ends the program and motor commands.
- [ ] Driver can repeat a short route at 30% power.

Keep 30% power until these pass. Later raise it gradually. Remove wheel-test controls from the final competition TeleOp after commissioning.

## 5. Calibrate servos

### 5.1 Positional and continuous-rotation servos are different

| Type | FTC Java class | Command | Meaning |
|---|---|---|---|
| Positional | `Servo` | `setPosition(0.0 ... 1.0)` | Command a position within configured travel |
| Continuous rotation | `CRServo` | `setPower(-1.0 ... 1.0)` | Command rotation direction and approximate speed |

`Servo.setPosition(0.5)` is not a universal "stop" command. For a positional servo, it commands a midpoint position and can move the linkage immediately. Normalized positions are not guaranteed degrees. `Servo.getPosition()` reports the last commanded position, not a measured shaft position.

1. Read each servo model label and programmed mode.
2. Configure each hub port with the matching servo type.
3. Identify whether each intake side servo spins a roller or deploys a side arm.
4. The mechanism program in section 6 assumes **two continuous-rotation intake servos** and **one positional feeder**.
5. If the intake servos position arms, use section 6.5's changes instead.

### 5.2 Calibrate a positional feeder with `RoboGoatsServoTest.java`

Start with the horn/linkage disconnected or positioned so a midpoint command cannot bind the mechanism. Run only this test; the shooter stays unpowered. The servo will first receive its midpoint command after START.

```java
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name = "13965 Servo Calibration", group = "RoboGoats Tests")
public class RoboGoatsServoTest extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        // Change ONLY this name when testing a different positional servo.
        Servo servo = hardwareMap.get(Servo.class, "feeder");
        double position = 0.50;
        boolean lastUp = false;
        boolean lastDown = false;
        telemetry.addLine("START commands midpoint. Disconnect linkage first.");
        telemetry.update();
        waitForStart();
        if (isStopRequested()) return;
        servo.setPosition(position);

        while (opModeIsActive()) {
            if (gamepad1.dpad_up && !lastUp) position += 0.01;
            if (gamepad1.dpad_down && !lastDown) position -= 0.01;
            position = Range.clip(position, 0.05, 0.95);
            servo.setPosition(position);
            lastUp = gamepad1.dpad_up;
            lastDown = gamepad1.dpad_down;
            telemetry.addData("Commanded position", "%.2f", position);
            telemetry.addLine("One D-pad press = 0.01. STOP if binding occurs.");
            telemetry.update();
            idle();
        }
    }
}
```

1. Run with linkage detached and confirm small steps work.
2. STOP. Attach the linkage at a known suitable orientation with power off.
3. Restart only if the midpoint is mechanically reachable. Otherwise change the initial test position to your established reachable value.
4. Move in small steps toward the **rest/closed** position. Record it as `FEEDER_REST`.
5. Move toward the **feed/open** position. Record it as `FEEDER_FEED`.
6. Stay clear of mechanical end stops. The test's 0.05–0.95 range is only a software range, not proof that the linkage can reach every value.
7. Measure enough dwell time to pass one piece and enough return time to close fully.
8. Repeat at least ten cycles by hand-controlled commands before adding automatic timing.

A continuous-rotation feeder cannot reliably index to a repeatable position using `setPosition`. Use a suitable positional servo or add feedback and a different controller if that is your hardware.

### 5.3 Test continuous-rotation intake servos

1. With the intake clear, map one servo as `CRServo`.
2. Command a small power, such as `0.15`, only while a test button is held.
3. Command zero whenever the button is released.
4. Test the second servo separately.
5. Determine the direction that pulls a piece inward for each servo. Mirrored servos often need opposite directions.
6. Record directions; confirm both stop at zero. If a servo creeps at zero, check its neutral calibration and actual operating mode.

Use this pattern inside an active OpMode loop after mapping the servo:

```java
// Fragment: inside a loop, with intakeLeft already mapped as CRServo.
intakeLeft.setPower(gamepad1.a ? 0.15 : 0.0);
```

## 6. Add intake, shooter, and feeder

### 6.1 Test the intake motor first

1. Add the Expansion Hub and `intake_motor` to the robot configuration.
2. Select its exact motor type. Connect its encoder too if available, although open-loop intake power does not require encoder feedback.
3. In a small test OpMode, map it as `DcMotor`, set power zero, and use `RUN_WITHOUT_ENCODER`.
4. Apply `0.15` while a button is held, zero on release. Determine which sign collects a piece.
5. Reverse the motor's configured software direction if needed so positive power collects.
6. Test forward and reverse without a piece, then with one piece.
7. Add the two intake servos and repeat. Tune speed gradually; the rollers should cooperate rather than fight.

### 6.2 Commission the shooter separately

The example below uses `DcMotorEx` velocity control. It assumes a shooter motor connected to a REV hub motor port **with its encoder cable connected to the matching encoder port**.

1. Disconnect or disable the feeder and remove game pieces.
2. Verify that the flywheel is secure and the intended launch path is clear.
3. Configure `shooter_motor` with its exact motor model and gear ratio.
4. Map it as `DcMotorEx`. Set `FLOAT` at zero power so the flywheel can coast.
5. First run a low-power direction/encoder test. Start at `0.10` power with `RUN_WITHOUT_ENCODER`, increasing only enough to confirm motion.
6. Display `getCurrentPosition()` and `getVelocity()`. Confirm encoder readings change and positive shooter direction produces a positive reported velocity.
7. Stop and correct direction, encoder wiring, or motor type before tuning speed.
8. Switch to `RUN_USING_ENCODER` and `setVelocity(targetTicksPerSecond)` for repeatable speed control. Do not set open-loop power elsewhere in the same loop.
9. Begin at a modest measured speed below the motor's capability. Record requested and measured speed without pieces.
10. Feed one piece only after speed settles. Watch the speed dip and recovery.

`setVelocity(double)` and `getVelocity()` use **encoder ticks per second**, not RPM.

```text
motor_output_ticks_per_rev = encoder counts for one motor gearbox output revolution
flywheel_revs_per_motor_output_rev = external transmission speed ratio

target_ticks_per_second =
    target_flywheel_RPM / 60
    / flywheel_revs_per_motor_output_rev
    * motor_output_ticks_per_rev
```

Use the manufacturer's counts with the same shaft reference as this formula. A direct-drive flywheel has an external ratio of 1. A flywheel running twice the motor output speed has a ratio of 2. Verify the physical gearing; do not assume every goBILDA motor has the same encoder counts per output revolution.

The hub's built-in velocity controller is the first approach. If speed cannot track a reasonable target, inspect battery, encoder readings, configured motor type, load, and achievable speed before changing PIDF coefficients.

### 6.3 Feeder behavior

The feeder should have explicit states:

```text
REST -> PUSH -> RETURN -> REST
```

- A new button press requests one cycle.
- Feed only after shooter velocity remains close to its target for a short continuous interval.
- During PUSH and RETURN, continue updating driving, intake, and shooter every loop.
- Ignore extra feed requests until the current cycle finishes.
- Stop feeding and command rest when shooter enable is released or the operator cancels.
- Speed readiness permits feeding; it does not prove a piece is present or that a jam cannot occur.

Do not put `sleep(500)` in the main driving loop to operate the feeder. A timer/state machine lets the robot keep responding.

### 6.4 Create `RoboGoatsMechanismTest.java`

This complete **stationary mechanism test** does not drive the robot. It assumes CR intake servos and a positional feeder. Three guards start disabled: enable the intake after checking directions, the shooter after determining a target, and feeding after servo calibration. Calibrate one feature at a time.

```java
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp(name = "13965 Mechanism Test", group = "RoboGoats Tests")
public class RoboGoatsMechanismTest extends LinearOpMode {
    // Enable individually only after the relevant commissioning steps.
    private static final boolean INTAKE_VERIFIED = false;
    private static final boolean SHOOTER_VERIFIED = false;
    private static final boolean FEEDER_CALIBRATED = false;

    private static final double SHOOTER_TPS = 0;       // MEASURE, must be > 0
    private static final double SPEED_TOLERANCE_TPS = 0; // MEASURE, must be > 0
    private static final double FEEDER_REST = 0.50;    // CALIBRATE
    private static final double FEEDER_FEED = 0.55;    // CALIBRATE
    private static final double PUSH_SECONDS = 0.20;  // CALIBRATE
    private static final double RETURN_SECONDS = 0.25;// CALIBRATE
    private enum FeedState { REST, PUSH, RETURN }

    @Override
    public void runOpMode() throws InterruptedException {
        DcMotor intake = hardwareMap.get(DcMotor.class, "intake_motor");
        CRServo left = hardwareMap.get(CRServo.class, "intake_left");
        CRServo right = hardwareMap.get(CRServo.class, "intake_right");
        DcMotorEx shooter = hardwareMap.get(DcMotorEx.class, "shooter_motor");
        Servo feeder = hardwareMap.get(Servo.class, "feeder");

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
            if (FEEDER_CALIBRATED) feeder.setPosition(FEEDER_REST);
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
                    state = FeedState.PUSH;
                    stateStarted = now;
                    withinSpeedSince = -1; // Require recovery before another feed.
                } else if (state == FeedState.PUSH && now - stateStarted >= PUSH_SECONDS) {
                    state = FeedState.RETURN;
                    stateStarted = now;
                } else if (state == FeedState.RETURN && now - stateStarted >= RETURN_SECONDS) {
                    state = FeedState.REST;
                }

                if (FEEDER_CALIBRATED) {
                    feeder.setPosition(state == FeedState.PUSH ? FEEDER_FEED : FEEDER_REST);
                }
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
            if (FEEDER_CALIBRATED) feeder.setPosition(FEEDER_REST);
        }
    }
}
```

Important behavior:

1. All five hardware names must exist before this particular test can INIT. For earlier single-device tests, map only the device under test.
2. The three false guards intentionally prevent actuation. Set only the guard for the feature you have commissioned to `true`.
3. `SHOOTER_TPS = 0` prevents accidental spin-up. Replace it with your measured initial target. Set a measured tolerance appropriate for that target; a starting experiment might be approximately 5% of the target, then tune from logs.
4. Set calibrated feeder positions and times before setting `FEEDER_CALIBRATED = true`. The displayed numbers are examples, not verified positions.
5. Assign a second controller to User 2. Without it, `gamepad2` inputs will not operate the mechanisms.
6. Hold Y to spin up; wait for `Shooter ready = true`; tap A to feed once.
7. Holding A does not repeat. Pressing A before readiness does not queue a shot; release and press again after readiness.
8. Releasing Y stops shooter power and returns the feeder to rest. The flywheel may coast.
9. Holding B cancels outputs. If the other controls remain held when B is released, those hold controls can resume; release controls before resuming a test. Driver Station STOP ends the OpMode.

### 6.5 If the intake side servos are positional

Change the mechanism example before running it:

1. Replace the `CRServo` import with `Servo` if no CR servos remain.
2. Map `left` and `right` using `Servo.class`.
3. Remove their `CRServo.Direction` lines and all `setPower` calls on these two objects, including initialization and `finally`.
4. Calibrate left/right **stowed** and **deployed** positions independently with the servo test. Mirrored positions are not necessarily identical.
5. Declare four calibrated constants: `LEFT_STOW`, `LEFT_DEPLOY`, `RIGHT_STOW`, `RIGHT_DEPLOY`.
6. Add a calibration guard for these arms, initially false. Do not command an unverified position at INIT.
7. Once calibrated, use a deliberate operator control for deployment. For example, hold gamepad 2 right bumper to deploy, release to stow:

```java
// Fragment: left and right are positional Servo objects; constants are calibrated.
if (ARMS_CALIBRATED) {
    boolean deployed = gamepad2.right_bumper && !cancel;
    left.setPosition(deployed ? LEFT_DEPLOY : LEFT_STOW);
    right.setPosition(deployed ? RIGHT_DEPLOY : RIGHT_STOW);
}
```

8. Add a timed deployment state if the intake motor must wait for arms to open. A commanded servo position does not prove the arm has arrived.
9. Choose a shutdown position that your actual linkage can reach without collision. Returning to stow may itself cause movement.

### 6.6 Mechanism acceptance checks

- [ ] Intake collects in the intended direction and reverses on request.
- [ ] Intake motor and both side servos stop when their hold control is released.
- [ ] Positional arms, if used, reach both positions without binding.
- [ ] Shooter encoder reports believable changing velocity.
- [ ] Shooter speed settles near the requested target.
- [ ] Feeder refuses to start while shooter is below readiness.
- [ ] Each A press gives one feed cycle.
- [ ] Holding A never produces repeated cycles.
- [ ] Shooter recovers before a second cycle is permitted.
- [ ] Cancel and STOP behave as expected in every feeder state.

## 7. Combine driving and mechanisms

### 7.1 Make one TeleOp loop

After the separate tests pass:

1. Create `RoboGoatsTeleOp.java` by copying the drive program into `TeamCode`.
2. Rename its public class to `RoboGoatsTeleOp` and its annotation to `13965 RoboGoats TeleOp`.
3. Remove the drive program's bench wheel-test branch and its left-bumper test telemetry.
4. Add the verified mechanism constants, enum, imports, hardware mappings, directions, and initialization from section 6.
5. Declare the mechanism state variables once before `waitForStart()`.
6. Use **one** `waitForStart()` and **one** `while (opModeIsActive())` loop.
7. Inside that loop, run the drive calculations, mechanism state update, and telemetry update in sequence. Each pass updates every subsystem.
8. Combine cleanup into one `finally` block that stops all motors and CR servos and commands only calibrated, appropriate servo positions.
9. Build before deploying. Fix duplicate variable names and imports if copying code.
10. Test combined controls at low drive power, first without game pieces.

Do not nest the entire mechanism `while` loop inside the drive loop. Do not call one OpMode's `runOpMode()` from another. Do not launch hardware-control threads to make the mechanisms operate at the same time; a short repeating main loop is sufficient here.

### 7.2 Suggested final control map

| Gamepad | Control | Action |
|---|---|---|
| 1 | Left stick | Forward/backward and strafe |
| 1 | Right stick X | Turn |
| 1 | Right bumper | Slow drive |
| 2 | Right trigger | Collect |
| 2 | Left trigger | Reverse intake, priority over collect |
| 2 | Hold Y | Shooter enabled |
| 2 | Tap A | Request one feed cycle |
| 2 | Hold B | Mechanism cancel |
| 2 | Right bumper | Deploy positional intake arms, if applicable |
| 2 | Left stick Y | Future slide manual command |
| 2 | D-pad | Future slide presets |
| 2 | Left bumper | Future explicit slide homing request |

The standalone slide test in section 8 uses gamepad 1; change those inputs to gamepad 2 during integration. Keep driver vision-assist controls distinct from the slow-drive control.

### 7.3 Organize after the combined version works

Suggested eventual files:

```text
teamcode/
  RoboGoatsTeleOp.java
  RoboGoatsAuto.java
  RobotConstants.java
  DriveSubsystem.java
  IntakeSubsystem.java
  ShooterSubsystem.java
  FeederSubsystem.java
  SlideSubsystem.java
  VisionSubsystem.java
  LocalizationSubsystem.java
```

Subsystem classes hold hardware and expose short `update(...)` and `stop()` methods. Only OpModes need `@TeleOp` or `@Autonomous`. Centralize calibrated values in `RobotConstants` once repeated copies become difficult to maintain. Preserve simple test OpModes for troubleshooting.

## 8. Add the two-stage linear slide

### 8.1 Define the mechanism before writing position control

One motor driving two slide stages is still one motor to program. The relationship between motor rotation and final carriage travel depends on the rigging; two stages do **not** automatically mean a factor of two.

1. Install the motor power and encoder cable at the same numbered motor/encoder port.
2. Map `slide_motor` as `DcMotorEx` with the correct motor type.
3. Add a bottom home/limit sensor named `slide_bottom` that the software can read as `TouchSensor`.
4. Add an independent top limit sensor if practical. The example below uses a measured encoder upper limit; an additional physical upper sensor improves fault coverage.
5. Arrange wiring so it cannot enter the moving slide, belts, strings, intake, or shooter.
6. Support the carriage during initial tests. `BRAKE` resists motion but does not guarantee a vertical slide will hold up a load, especially after STOP or loss of power. Use suitable counterbalance/support as required by the mechanism.
7. Determine which motor direction makes encoder counts increase as the carriage rises.
8. Establish usable travel that stays clear of both mechanical stops and cable limits.

### 8.2 Establish the position scale

```text
ticks_per_mm = motor_output_ticks_per_revolution
               / carriage_travel_mm_per_motor_output_revolution

target_ticks = target_height_mm * ticks_per_mm
```

Measure **final carriage movement**, not just first-stage extension. You can also record encoder changes across a known short movement to check the calculated scale. Repeat at multiple heights; spool winding layers can change travel per revolution.

Record:

- Bottom-switch position, defining zero.
- Small initial test extension.
- Maximum allowed extension with margin before binding.
- Stowed and operating heights.
- Conservative upward and downward power limits.

### 8.3 Why homing is required

`STOP_AND_RESET_ENCODER` means "call the current position zero." It does not locate the bottom. Never reset the slide encoder at arbitrary startup height and assume the slide is retracted.

Use this startup sequence:

1. INIT: motor power zero; position is **UNHOMED**.
2. Operator holds the explicit home control.
3. Retract slowly toward the bottom sensor.
4. When the sensor remains pressed briefly, stop the motor, reset the encoder, and mark the slide **HOMED**.
5. If homing times out or is canceled, stop and report a fault.
6. Once homed, clamp requested targets between zero and your measured upper limit.

Before using the homing motor command, display `slide_bottom.isPressed()` in a sensor-only test. Press and release the sensor by hand and verify the reported value. A reversed, shorted, or stuck switch can falsely declare home; inspect unexpected readings and test switch operation before each calibration session.

### 8.4 Create `RoboGoatsSlideTest.java`

This standalone test requires both slide devices. It refuses to run until you enter a measured limit and confirm commissioning. It uses a home state machine and simple encoder target control. It is an initial bench controller; tune holding behavior and add fault monitoring before full-speed use.

```java
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name = "13965 Slide Test", group = "RoboGoats Tests")
public class RoboGoatsSlideTest extends LinearOpMode {
    private static final boolean SLIDE_COMMISSIONED = false;
    private static final int MAX_TICKS = 0; // MEASURE: initially a short test range
    private static final double HOME_TIMEOUT_SECONDS = 3.0; // CALIBRATE
    private static final double MOVE_TIMEOUT_SECONDS = 3.0; // CALIBRATE
    private enum State { UNHOMED, HOMING, READY, MOVING, FAULT }

    @Override
    public void runOpMode() throws InterruptedException {
        DcMotorEx slide = hardwareMap.get(DcMotorEx.class, "slide_motor");
        TouchSensor bottom = hardwareMap.get(TouchSensor.class, "slide_bottom");
        slide.setPower(0);
        slide.setDirection(DcMotor.Direction.FORWARD); // VERIFY: up must be positive
        slide.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        if (!SLIDE_COMMISSIONED || MAX_TICKS <= 0) {
            telemetry.addLine("Enter measured limit; verify directions and switch first.");
            telemetry.update();
            waitForStart();
            return;
        }

        State state = State.UNHOMED;
        ElapsedTime clock = new ElapsedTime();
        double started = 0;
        double switchSince = -1;
        int target = 0;
        boolean lastUp = false, lastDown = false;
        telemetry.addLine("Hold LB to home; D-pad up/down moves by 50 ticks; B cancels.");
        telemetry.update();
        waitForStart();
        try {
            while (opModeIsActive()) {
                double now = clock.seconds();
                int position = slide.getCurrentPosition();
                boolean upEdge = gamepad1.dpad_up && !lastUp;
                boolean downEdge = gamepad1.dpad_down && !lastDown;
                lastUp = gamepad1.dpad_up;
                lastDown = gamepad1.dpad_down;

                if (gamepad1.b) state = State.FAULT;
                if (state == State.UNHOMED && gamepad1.left_bumper) {
                    slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    state = State.HOMING;
                    started = now;
                }
                if (state == State.HOMING) {
                    if (!gamepad1.left_bumper || now - started > HOME_TIMEOUT_SECONDS) {
                        state = State.FAULT;
                    } else if (bottom.isPressed()) {
                        slide.setPower(0);
                        if (switchSince < 0) switchSince = now;
                        if (now - switchSince >= 0.05) {
                            slide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                            slide.setTargetPosition(0);
                            slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                            target = 0;
                            state = State.READY;
                        }
                    } else {
                        switchSince = -1;
                        slide.setPower(-0.15); // VERIFY slow retract direction first
                    }
                } else if (state == State.READY || state == State.MOVING) {
                    // Do not accept a new target until the previous move completes.
                    if (state == State.READY && (upEdge || downEdge)) {
                        target = Range.clip(target + (upEdge ? 50 : -50), 0, MAX_TICKS);
                        slide.setTargetPosition(target);
                        slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        state = State.MOVING;
                        started = now;
                    }
                    if (position < -30 || position > MAX_TICKS + 30) state = State.FAULT;
                    if (state == State.MOVING) {
                        if (Math.abs(position - target) <= 10) state = State.READY;
                        else if (now - started > MOVE_TIMEOUT_SECONDS) state = State.FAULT;
                    }
                    // RUN_TO_POSITION uses positive power magnitude for either direction.
                    double power = 0.25; // CALIBRATE for actual loaded slide
                    if (bottom.isPressed() && target <= position) power = 0;
                    slide.setPower(state == State.FAULT ? 0 : power);
                }
                if (state == State.FAULT || state == State.UNHOMED) slide.setPower(0);
                telemetry.addData("State", state);
                telemetry.addData("Position / target", "%d / %d", position, target);
                telemetry.addData("Bottom pressed", bottom.isPressed());
                telemetry.addLine("FAULT: STOP, inspect/support slide, then restart to home.");
                telemetry.update();
                idle();
            }
        } finally {
            slide.setPower(0);
        }
    }
}
```

1. Verify motor/encoder signs and switch readings with short, separate tests before setting `SLIDE_COMMISSIONED` true.
2. Enter a conservative **measured short test limit** in `MAX_TICKS`; do not immediately use the full mechanical stroke.
3. Choose a home timeout long enough for your starting distance at low speed but short enough to stop a stalled motor promptly. The three-second value is an example.
4. Run the test, hold LB, and watch the slide retract. Release LB or press B if anything is wrong. Releasing LB during homing faults and stops the test.
5. Once home is detected, release LB. Tap D-pad up once. Confirm about 50 encoder ticks of upward motion.
6. A 50-tick step may be too large or small for your mechanism; adjust it after measuring the scale.
7. Tap down and verify the bottom limit prevents further retraction.
8. Gradually expand the test range after inspecting mechanical travel and wiring.
9. Test a motion that cannot reach its target without deliberately jamming the mechanism: for example, set a short timeout during a supported, unloaded test. Confirm timeout enters FAULT and power becomes zero.
10. Restore the appropriate timeout, repeat under expected load, and watch for overheating or sag.

This controller's holding power, 10-tick tolerance, and 30-tick overtravel fault margin are examples requiring tuning. If the slide cannot hold its load, use an appropriate feedback/feedforward controller and mechanical support. Do not simply increase power until it overheats.

### 8.5 Integrate the slide

1. Convert the test into a `SlideSubsystem` that updates once per TeleOp loop.
2. Keep homing, target clamping, move timeouts, and FAULT behavior.
3. Change its controls to gamepad 2 and reserve distinct inputs for home and preset selection.
4. For manual control, turn stick input into a target change per second using measured loop elapsed time; clamp the resulting target. Avoid loop-rate-dependent increments.
5. Add preset heights only after repeated homing gives repeatable positions.
6. Add a collision rule: permit feeding/shooting only at a verified compatible slide height if the mechanism requires it.
7. Reduce drive speed with the slide extended if stability testing calls for it.
8. Do not use slide-mounted vision geometry until homing is complete and slide position is credible.

## 9. Add Pinpoint and two odometry pods

### 9.1 Install and configure

1. Mount one pod to measure forward travel and the other to measure sideways travel. Their measuring directions should be perpendicular.
2. Ensure the pod wheels stay in contact with the floor throughout normal motion.
3. Connect the pods to the Pinpoint's correct X/forward and Y/lateral encoder inputs, following the exact pod and Pinpoint manuals.
4. Mount the Pinpoint in its prescribed orientation on the rigid chassis, not on the moving slide. Follow the manual for your actual hardware revision.
5. Connect Pinpoint to a compatible hub I²C port, using the manufacturer's wiring.
6. In the hardware configuration, select the goBILDA Pinpoint device type and name it `pinpoint`.
7. You do not configure the two pods as two drive motors. Their encoders connect to the Pinpoint, which reports a fused pose.

### 9.2 Start from the included SDK sample

Use [SensorGoBildaPinpoint.java](FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/SensorGoBildaPinpoint.java):

1. Copy it into `TeamCode` as `RoboGoatsPinpointTest`.
2. Change the package to `org.firstinspires.ftc.teamcode` if Android Studio did not do it.
3. Rename the public class to match the file.
4. Remove `@Disabled`.
5. Set the annotation name to `13965 Pinpoint Test`.
6. Keep the **SDK-provided** import:

```java
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
```

This SDK already includes the driver. Do not add a second downloaded driver class unless a documented hardware compatibility issue requires it.

### 9.3 Replace the sample's geometry

The sample's `-84` and `-168` millimeter offsets describe a different robot. Replace them with RoboGoats measurements.

- Choose a tracking point, typically the chassis center, and mark it.
- **X pod offset:** sideways distance from that point to the forward-tracking pod. Left is positive; right is negative.
- **Y pod offset:** forward/backward distance from that point to the sideways-tracking pod. Forward is positive; backward is negative.
- Measure perpendicular to each wheel's rolling direction, using the wheel contact/tracking location.

Update these calls in `configurePinpoint()`:

```java
// Fragment: replace these locally defined example variables with measured values.
pinpoint.setOffsets(xPodSidewaysOffsetMm, yPodForwardOffsetMm, DistanceUnit.MM);
pinpoint.setEncoderResolution(
        GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
pinpoint.setEncoderDirections(
        GoBildaPinpointDriver.EncoderDirection.FORWARD,
        GoBildaPinpointDriver.EncoderDirection.FORWARD);
```

Select `goBILDA_SWINGARM_POD` if that is your pod model. Use the actual encoder resolution for other pods. For this SDK the reverse enum is **`REVERSED`**, not `REVERSE`.

Configure the device each OpMode initialization. Request `resetPosAndIMU()` once while stationary; wait for calibration to finish and verify `READY` after calling `update()`. Do not reset or recalibrate continuously in the active control loop. See the [Pinpoint user guide](https://www.gobilda.com/content/user_manuals/3110-0002-0001%20User%20Guide.pdf).

Add status telemetry to the test's loop after `pinpoint.update()`:

```java
telemetry.addData("Pinpoint status", pinpoint.getDeviceStatus());
```

Keep the chassis still during calibration. In an eventual autonomous OpMode, refuse to drive from pose feedback until the status is `GoBildaPinpointDriver.DeviceStatus.READY`.

### 9.4 Calibrate by pushing, without motor commands

1. Run the telemetry-only Pinpoint test on the floor.
2. Wait until status is READY and stationary readings are stable.
3. Push straight forward a measured distance; X should increase.
4. Push straight left; Y should increase.
5. If the relevant coordinate decreases, reverse that pod's encoder direction.
6. If the wrong coordinate changes most, inspect X/Y connections and pod orientation.
7. Rotate counterclockwise about the tracking point: heading should increase, and large false translation indicates geometry/direction problems.
8. Compare measured forward and sideways distances to reported distances.
9. Drive/push a rectangular route and return to the start. Record position and heading error.
10. Fix contact, wiring, model selection, offsets, and signs before attempting software compensation.

The unit/sign convention and acceptance sequence follow the manufacturer guide; your desired accuracy should be recorded from repeated tests rather than assumed from a single run.

### 9.5 Use the readings in TeleOp

Call `pinpoint.update()` once per loop, then read the pose:

```java
// Fragment: imports are in the SDK Pinpoint sample.
pinpoint.update();
Pose2D pose = pinpoint.getPosition();
double xMm = pose.getX(DistanceUnit.MM);
double yMm = pose.getY(DistanceUnit.MM);
double headingRad = pose.getHeading(AngleUnit.RADIANS);
```

First display these values while driving manually. Pinpoint estimates motion; it does not command the motors or create autonomous paths by itself.

For optional field-relative driving, use a documented heading reference and radians. Pinpoint's natural robot axes are forward and **left**; the drive mixer above accepts forward and **right**. Convert deliberately:

```text
fieldForward = -left_stick_y
fieldLeft = -left_stick_x

robotForward = cos(h) * fieldForward + sin(h) * fieldLeft
robotLeft    = -sin(h) * fieldForward + cos(h) * fieldLeft

mixerForward = robotForward
mixerRight = -robotLeft
```

Here `h` is the robot's counterclockwise heading relative to the chosen driver field-forward direction. Establish that reference while stationary. Test at 0°, 90°, 180°, and 270° before increasing drive power.

## 10. Add the front Limelight

### 10.1 Start with one camera

Confirm model and power requirements. The **Limelight 3A** uses USB power and USB networking; its manufacturer specifies up to 4 W and recommends the Control Hub's USB 3.0 host port. See [Limelight 3A quick-start](https://docs.limelightvision.io/docs/docs-limelight/getting-started/limelight-3a).

1. Connect only the front camera first.
2. Connect it to your computer and access its settings with Limelight Hardware Manager or the documented local web interface.
3. Record firmware and export any existing pipelines before firmware changes.
4. Give it a distinctive hostname, such as `limelight-front`.
5. If the settings request team number, enter `13965`. FTC code in this guide uses the configured USB device endpoint; do not invent an FRC-style `10.TE.AM.x` address from your five-digit team number.
6. Create and save a game-piece detection pipeline. Use pipeline **0** in this guide.
7. Connect the camera to the Control Hub USB host port with a data-capable cable.
8. Scan/configure it in the Driver Station. Name the device `ll_front` and verify its endpoint settings using the SDK sample's notes.
9. Do not confuse the Control Hub USB network interface address with the camera's own address. The [SDK Limelight sample](FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/SensorLimelight3A.java) explains this distinction.

### 10.2 Build a pipeline that actually detects your pieces

1. Put representative game pieces in the camera view at realistic distances.
2. Start with a color pipeline if color reliably separates pieces from the field.
3. Adjust exposure/gain and color thresholds while observing the threshold mask.
4. Filter tiny reflections and irrelevant shapes by area and geometry.
5. Select the target sorting method intentionally, such as the piece closest to the intake's desired image location.
6. Record the crosshair/aim point. The image center is not automatically the intake pickup point.
7. Test no piece, one piece, several pieces, partial occlusion, and lighting changes.
8. Confirm no-target results are invalid rather than a stale old target.
9. If using a neural detector, verify its trained classes actually include the pieces you need. A detector is not automatically trained for a new FTC game.

### 10.3 Create `RoboGoatsVisionTest.java`

This complete diagnostic reads one camera and never commands motion. Change `CAMERA_NAME` and `PIPELINE` when testing the other cameras individually.

```java
package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp(name = "13965 Vision Test", group = "RoboGoats Tests")
public class RoboGoatsVisionTest extends LinearOpMode {
    private static final String CAMERA_NAME = "ll_front";
    private static final int PIPELINE = 0;

    @Override
    public void runOpMode() throws InterruptedException {
        Limelight3A camera = hardwareMap.get(Limelight3A.class, CAMERA_NAME);
        camera.setPollRateHz(30);
        camera.pipelineSwitch(PIPELINE);
        camera.start();
        ElapsedTime clock = new ElapsedTime();
        double lastTimestamp = Double.NaN;
        double lastFrameChangedAt = -100;
        telemetry.setMsTransmissionInterval(100);
        telemetry.addLine("Vision telemetry only; no motor commands.");
        telemetry.update();
        try {
            waitForStart();
            while (opModeIsActive()) {
                LLResult result = camera.getLatestResult();
                if (result != null && result.getTimestamp() != lastTimestamp) {
                    lastTimestamp = result.getTimestamp();
                    lastFrameChangedAt = clock.seconds();
                }
                boolean usable = camera.isConnected() && result != null
                        && result.isValid() && result.getPipelineIndex() == PIPELINE
                        && result.getStaleness() < 250
                        && clock.seconds() - lastFrameChangedAt < 0.25;
                telemetry.addData("Camera", CAMERA_NAME);
                telemetry.addData("Connected", camera.isConnected());
                telemetry.addData("Usable target", usable);
                if (result != null) {
                    telemetry.addData("Pipeline", result.getPipelineIndex());
                    telemetry.addData("Receipt age ms", result.getStaleness());
                    telemetry.addData("Frame age estimate s", clock.seconds() - lastFrameChangedAt);
                    if (usable) {
                        telemetry.addData("tx / ty deg", "%.2f / %.2f", result.getTx(), result.getTy());
                        for (LLResultTypes.FiducialResult tag : result.getFiducialResults()) {
                            telemetry.addData("Tag ID", tag.getFiducialId());
                        }
                    }
                }
                telemetry.update();
                idle();
            }
        } finally {
            camera.stop();
        }
    }
}
```

`getLatestResult()` can be null. `isValid()` alone does not mean data is fresh or from the requested pipeline. This example checks both receipt age and whether the camera-local result timestamp keeps changing. These are initial timeout choices; tune them against actual pipeline frame rate and latency.

The method names are supported by this checkout's SDK 12.0.0 sources. For general pipeline/result usage, see [Limelight's FTC Java guide](https://docs.limelightvision.io/docs/docs-limelight/apis/ftc-programming).

### 10.4 Add driver assistance after telemetry works

Start with **turn assistance only**, while the driver controls translation:

1. Reserve a hold button for assistance, for example gamepad 1 left trigger after wheel-test mode has been removed.
2. Choose a target and calibrated `desiredTxDegrees` corresponding to the intake alignment point.
3. Compute `error = measuredTxDegrees - desiredTxDegrees`.
4. Compute a small proportional turn correction, clipped to a low maximum such as 0.15 turn command.
5. On a stationary supported robot, move a target to the right and check the correction sign. On the floor, the robot must turn so the error decreases. Stop immediately if it turns away.
6. Verify the sign for your camera orientation; display rotation/mirroring does not prove the result axes changed.
7. Use the correction only while the assist button is held, the driver is not deliberately turning, and the target passes all freshness/pipeline checks.
8. On target loss, set the **assist correction** to zero immediately and retain normal manual drive.
9. Apply the correction before wheel-power normalization and final speed scaling.
10. Add a small deadband so noise near the goal does not cause oscillation.

Do not automatically drive toward a piece using only `tx`. It supplies an angle, not a validated pickup distance. Add calibrated range/ground-plane geometry and a bounded approach state before forward automation. Keep manual override available.

## 11. Add the slide-mounted and AprilTag Limelights

### 11.1 Verify three-camera support before depending on it

**Three simultaneous USB Limelights on your FTC setup are a capability to verify, not an assumption in this guide.** Limelight's published changelog describes unique USB IDs/subnets for multiple cameras, but another entry explicitly describes a single USB Limelight on an FTC Control Hub. Those statements do not establish a validated three-camera FTC configuration for your exact hardware and software. See the [official changelog](https://docs.limelightvision.io/docs/docs-limelight/software-change-log).

Proceed as a staged integration experiment:

1. Record exact camera models, Limelight firmware, Control Hub OS, and FTC SDK/app versions.
2. Test each camera by itself with section 10's diagnostic.
3. Give each a distinct hostname and, where supported, a distinct USB ID while connected individually. Record the actual resulting endpoint addresses; do not guess them.
4. Confirm with current vendor guidance that the combination is supported on your FTC controller.
5. Calculate USB port count and power budget. Three LL3A cameras can require up to **12 W total**, derived from the manufacturer's 4 W per-camera maximum; this excludes hub overhead. Do not assume a passive splitter provides enough power.
6. Use a properly rated, competition-appropriate powered USB arrangement if required. Follow its documented power input and prevent unintended backfeeding. A USB hub adds connections; it does not by itself solve address conflicts, power limitations, or Android interface support.
7. Add a second camera, rescan the configuration, and make sure each maps to its own endpoint and name.
8. Write a two-camera telemetry diagnostic using one `Limelight3A` object per mapped device. Use section 10's independent timestamp/freshness tracking for each object.
9. Start each once before the loop, read each once per loop, and stop all started cameras in cleanup. Begin with modest polling rates, such as 20–30 Hz each; measure responsiveness.
10. Cover one lens at a time and confirm the correct camera's target status changes.
11. Unplug one camera during a stationary diagnostic. Confirm its result becomes unusable while the others remain distinguishable.
12. Repeat after power cycling to check stable enumeration and endpoint mapping.
13. Add the third camera only after the two-camera configuration is stable.
14. Test with the slide moving, shooter running, and a match-length operating period. Watch for disconnects, power resets, excessive loop times, stale frames, and Driver Station latency.

If the combination fails, retain a working single-camera configuration, switch pipelines for different tasks where the physical view permits, or revise the camera arrangement with vendor guidance. Java variable names alone cannot make unsupported hardware enumeration work.

After the platform is validated, the mappings are:

```java
// Fragment: only after all three are installed and independently verified.
Limelight3A front = hardwareMap.get(Limelight3A.class, "ll_front");
Limelight3A slideCamera = hardwareMap.get(Limelight3A.class, "ll_slide");
Limelight3A tags = hardwareMap.get(Limelight3A.class, "ll_tags");
```

### 11.2 Slide-mounted camera: geometry changes with height

The camera moves with the slide. A distance calibration made at one height will generally be wrong at another.

1. Start with the slide homed and stationary at one known height.
2. Mount the camera so it really covers the rear/side collection area. One fixed view may not cover both sides plus the rear; measure the actual view rather than assuming it.
3. Record camera orientation relative to the robot, including whether it faces backward or sideways.
4. Calibrate detection and crosshair at that height.
5. Place a piece at measured robot-relative positions and record `tx`, `ty`, slide height, and the desired pickup point.
6. Repeat at each intended operating height.
7. First restrict vision assistance to those calibrated, stationary presets.
8. For continuous-height geometry later, calculate a camera-to-robot transform from measured slide carriage height and fixed mount orientation.
9. Project a calibrated camera ray onto the floor plane to estimate a piece location, or use a measured lookup table. Reject near-horizon/ill-conditioned rays and unreasonable ranges.
10. Transform detections into a common robot frame before comparing front and rear/side cameras. A target on a rear camera's image-right is not automatically on the robot's right.
11. Account for height at **image capture time** when moving. Until you have synchronized position history and latency handling, disable position-based assistance while the slide moves and wait for settling plus a new frame.
12. Reject results if the slide is unhomed, faulted, outside calibrated heights, or the image is stale.

Do not feed the slide camera's pose into chassis localization using one fixed mount transform. The mount transform must follow slide motion. A moving camera can still provide useful detections before full geometric localization is implemented.

### 11.3 Upward-facing AprilTag camera

1. Install it rigidly on the chassis where it has a clear view of the tags you need.
2. Point it upward only enough to see those tags over the required approach range. Verify near and far visibility physically.
3. Create an AprilTag pipeline, using pipeline **1** as this guide's example.
4. Configure the correct tag family and physical size for your actual tags.
5. Run `RoboGoatsVisionTest` with `CAMERA_NAME = "ll_tags"` and `PIPELINE = 1`.
6. Show one known tag, verify its ID, then move it through the intended viewing range.
7. Add an allowlist of intended tag IDs before any automatic aiming action. A valid tag may belong to a target you do not want.
8. Use the chosen tag's own fiducial-result angles if several tags are visible; do not check one tag ID and blindly use aggregate `getTx()` belonging to a different selected target.
9. Start with bounded, hold-to-enable turn assistance, using the same override and loss behavior as the front camera.
10. For field pose, load a map containing the correct stationary tag poses, configure the camera's measured robot-relative translation and rotation, and enable the required 3D mode.
11. Validate the reported robot pose at several measured field positions and headings before using it to correct odometry.

**This checkout's README explicitly says BIOBUZZ AprilTags move and are unsuitable for absolute field localization.** A moving game tag can still support relative targeting, but it cannot be treated as a fixed field landmark. For absolute pose, use only genuinely stationary known tags and a matching map. See the BIOBUZZ note in [README.md](README.md).

### 11.4 Combine AprilTags and Pinpoint carefully

1. Pick one field origin, axis convention, unit system, and heading reference.
2. Keep an explicit transform between Pinpoint's chosen start frame and the tag map's field frame.
3. Verify Limelight pose units and convert to your chosen millimeters/meters explicitly.
4. Supply properly transformed field yaw if using an IMU-assisted Limelight pose mode; raw Pinpoint zero is not automatically field-map zero.
5. Accept only stationary mapped tag IDs, fresh pipeline results, credible distances, and plausible poses.
6. Compare vision position/heading to odometry before applying any correction; log discrepancies.
7. Initially accept a correction only while the robot is stopped at a known location.
8. Later add a measured correction policy, including latency handling, outlier rejection, and bounded changes. Do not call `pinpoint.setPosition(...)` on every noisy frame without a policy.
9. On no tag, continue odometry. On Pinpoint faults, stop any autonomous action that requires trustworthy pose and report the fault.

## 12. Build autonomous movement

### 12.1 Start with short actions

1. Create `RoboGoatsAuto.java` in `TeamCode` with `@Autonomous`.
2. Reuse verified hardware directions and drivetrain mixing.
3. Initialize Pinpoint while stationary and wait for READY with a timeout.
4. Set the chosen starting pose once after calibration, using the same frame as your targets.
5. Press START and perform one short low-speed movement.
6. Stop, inspect measured pose error, and repeat from the same start.
7. Add one turn, then another translation. Validate each action before combining mechanisms.

Timed movement can check basic sequencing, but speed varies with battery, surface, and load. Use measured pose feedback for repeatable destinations once localization is validated.

### 12.2 Coordinate math for a move-to-pose controller

This is an algorithm sketch, not a complete Java OpMode. Use Pinpoint coordinates: X forward, Y left, counterclockwise heading positive.

```text
fieldErrorX = targetX - currentX
fieldErrorY = targetY - currentY

robotForwardError = cos(h) * fieldErrorX + sin(h) * fieldErrorY
robotLeftError = -sin(h) * fieldErrorX + cos(h) * fieldErrorY
headingError = wrap targetHeading - h to [-pi, +pi]

forwardCommand = clip(kPosition * robotForwardError, -maxTranslation, +maxTranslation)
rightCommand = clip(-kPosition * robotLeftError, -maxTranslation, +maxTranslation)
clockwiseCommand = clip(-kHeading * headingError, -maxTurn, +maxTurn)

normalize wheel commands using the mecanum mixer
```

1. Use radians for the trigonometry and heading error. Match gain units to your position units.
2. Begin with low maximum commands and one axis at a time.
3. Require position and heading errors to remain within tolerance for a short settle interval before declaring success.
4. Give every movement a timeout. On timeout, stop and report failure; do not proceed into an automatic shot from an unverified location.
5. Check OpMode STOP and localization health every iteration.
6. Set all drive powers to zero on success, failure, and final cleanup.
7. Test with the robot pointed 90° from the initial forward direction so an incorrect coordinate transform becomes obvious.

### 12.3 Sequence actions with explicit success/failure

Suggested initial autonomous states:

```text
WAIT_FOR_START
  -> MOVE_TO_TEST_POSE
  -> SETTLE
  -> SPIN_UP
  -> WAIT_FOR_SPEED
  -> FEED_ONE
  -> RETURN_FEEDER
  -> STOP

Any timeout or required-sensor fault -> ABORT -> stop outputs
```

1. Add the shooter only after movement is reliable.
2. Wait for verified speed readiness before feeding.
3. Use the feeder state machine's completion event, not just a button state.
4. Add a shot timeout and a limited number of attempts; avoid an endless retry loop.
5. Add slide movement only after homing and preset moves pass repeatedly.
6. Add vision targeting last, with explicit handling for no target and stale target.
7. Log state, elapsed time, pose, target pose, shooter speed, feeder state, slide state, and fault reason.
8. Repeat with realistic battery levels and full mechanism loads.

For longer paths, a maintained FTC path-following library may save work, but choose and verify a specific library/version against this SDK when you reach that stage. Pinpoint support, coordinate conventions, and tuning steps depend on the library. This guide does not assume one is installed.

## 13. Troubleshooting

| Symptom | Check first |
|---|---|
| OpMode absent from Driver Station | Class in `TeamCode`, correct annotation, no `@Disabled`, successful installation, correct program list |
| Old behavior after an edit | Saved file, successful build/deploy, correct Control Hub target and selected OpMode |
| INIT reports device not found | Exact configured name, device type, active configuration, connected hub |
| Android Studio does not see controller | USB-C data connection, controller powered, debugging authorization, cable |
| Wrong drive wheel moves | Motor name-to-port mapping |
| Forward works but strafe fails | Wheel roller arrangement, motor signs, mixer unchanged |
| Robot drives backward | Verify all four individual wheel directions, then retest forward |
| Mechanisms ignore buttons | Guards still false, zero shooter constants, gamepad 2 not assigned |
| Positional servo jumps or binds | Reachable initial position, horn alignment, calibrated endpoints, correct operating mode |
| CR servo creeps when released | Zero command, neutral calibration, actual CR mode |
| Shooter velocity stays zero | Encoder plugged into matching port, correct motor type, valid nonzero target |
| Shooter never ready | Target achievable, velocity sign, tolerance, encoder, load, battery |
| Feeder does nothing | Calibrated guard, shooter held on and ready, new A press rather than held A |
| Driving pauses during feeding | Remove blocking sleep or inner mechanism loop |
| Slide immediately claims home | Sensor stuck/shorted, incorrect polarity/type, switch already physically pressed |
| Slide moves away from home | Motor direction; stop before changing anything |
| Slide loses position after restart | Expected until homed; do not reset encoder at unknown height |
| Slide falls on STOP | Mechanical support/counterbalance, drivetrain backdrive; BRAKE is not a guarantee |
| Pinpoint translation wrong during turns | Pod directions, axis connections, measured offset signs |
| Pinpoint does not become READY | Pod wiring/status, stationary calibration, hardware revision compatibility |
| Camera result null | Correct mapping/endpoint, camera started, power, connection |
| Camera valid but old | Receipt age plus advancing frame timestamp; pipeline actually processing |
| Wrong camera data under a name | Duplicate endpoints/USB IDs, enumeration mapping; cover-one-lens test |
| Camera disconnects under load | USB power budget, cable retention, hub quality, electrical interference |
| Vision assist turns away | Camera/mixer axis sign; verify at low speed |
| Tag pose jumps around | Moving tags, wrong map/size, mount transform, units, latency, poor observations |
| Slide camera distance changes unexpectedly | Height-dependent transform, slide motion during capture, uncalibrated view |

When troubleshooting, return to the smallest working test: drive-only, one motor, one servo, Pinpoint-only, or one camera. Change one cause at a time and record what changed.

## 14. Team calibration record and milestones

### 14.1 Hardware record

| Item | RoboGoats value |
|---|---|
| Control Hub serial / OS | To record |
| Expansion Hub serial / firmware | To record |
| Driver Station version | To record |
| FTC SDK | 12.0.0 in this checkout |
| Drive motor exact model | To record |
| Intake motor exact model / collect direction | To record |
| Intake left servo model / mode / direction or endpoints | To record |
| Intake right servo model / mode / direction or endpoints | To record |
| Feeder servo model / operating mode | To record |
| Shooter motor model / external speed ratio | To record |
| Shooter output ticks/revolution | To record |
| Slide motor model / rigging arrangement | To record |
| Slide home sensor model / port | To record |
| Odometry pod model | To record |
| Pinpoint model / firmware | To record |
| Front / slide / tag camera models and firmware | To record |
| Camera hostnames / USB IDs / actual endpoints | To record |
| Verified multi-camera configuration | To record; do not assume |

### 14.2 Calibration values

| Value | Measured setting | Test date / tester |
|---|---|---|
| FL / FR / BL / BR directions | | |
| Normal / slow drive scale | Initial 0.30 / 0.15 | |
| Intake motor / CR servo power | | |
| Positional intake arm endpoints, if applicable | | |
| Feeder rest / feed positions | | |
| Feeder push / return duration | | |
| Shooter target ticks/s | | |
| Shooter tolerance / settle duration | | |
| Slide ticks/mm / maximum ticks | | |
| Slide presets | | |
| Slide homing power / timeout | | |
| Slide motion power / timeout / tolerance | | |
| Pinpoint X-pod sideways offset, mm | | |
| Pinpoint Y-pod forward offset, mm | | |
| Pinpoint pod directions | | |
| Camera mount translations / rotations | | |
| Front / slide / tag pipeline indices | | |
| Vision age limits and measured latency | | |
| Slide heights allowed for vision assistance | | |
| Autonomous pose tolerance / settle / timeout | | |

### 14.3 Milestone checklist

- [ ] M1: Drive-only program passes all motion and STOP checks.
- [ ] M2: Intake operates forward/reverse and stops on release.
- [ ] M3: All servo types identified and endpoints/directions recorded.
- [ ] M4: Shooter velocity is repeatable with recorded units and target.
- [ ] M5: Feeder completes one cycle per request with readiness interlock.
- [ ] M6: Combined TeleOp drives and operates mechanisms concurrently.
- [ ] M7: Slide homes, moves within limits, holds appropriately, and faults on timeout.
- [ ] M8: Pinpoint passes direction, distance, rotation, and return-to-start checks.
- [ ] M9: Each Limelight works alone with freshness checks.
- [ ] M10: Intended camera combination works after reboot and under mechanism load.
- [ ] M11: Driver assistance reduces target error and yields to manual control.
- [ ] M12: Autonomous moves to one pose and stops on success or failure.
- [ ] M13: Autonomous completes one verified mechanism sequence.

At each milestone, record code version, configuration name, measurements, test conditions, and remaining problems. Commit working changes in your usual version-control workflow before starting the next stage.

## 15. References

Local references match this repository's SDK version:

- [Project README and version notes](README.md)
- [TeamCode instructions](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/readme.md)
- [Mecanum/omni drive sample](FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/BasicOmniOpMode_Linear.java)
- [Limelight sample and configuration notes](FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/SensorLimelight3A.java)
- [Pinpoint sample](FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/SensorGoBildaPinpoint.java)

Official setup and hardware references checked when preparing this guide:

- [FTC: creating and running an Android Studio OpMode](https://ftc-docs.firstinspires.org/en/latest/programming_resources/tutorial_specific/android_studio/creating_op_modes/Creating-and-Running-an-Op-Mode-%28Android-Studio%29.html)
- [FTC: Android Studio prerequisites](https://ftc-docs.firstinspires.org/en/latest/programming_resources/android_studio_java/before_you_start/before-you-start.html)
- [REV: Control Hub specifications](https://www.revrobotics.com/rev-31-1595/)
- [FTC: Control and Expansion Hub tips](https://ftc-docs.firstinspires.org/en/latest/tech_tips/tech-tips/tech-tip-hub-tips/tech-tip-hub-tips.html)
- [goBILDA: Pinpoint user guide, model 3110-0002-0001](https://www.gobilda.com/content/user_manuals/3110-0002-0001%20User%20Guide.pdf)
- [Limelight: 3A quick-start](https://docs.limelightvision.io/docs/docs-limelight/getting-started/limelight-3a)
- [Limelight: FTC Java and Blockly API guide](https://docs.limelightvision.io/docs/docs-limelight/apis/ftc-programming)
- [Limelight: software changelog, including USB IDs and FTC support notes](https://docs.limelightvision.io/docs/docs-limelight/software-change-log)

Use the manual for the exact hardware revision you install. The snippets with calibration guards require robot measurements before actuation; code compilation alone cannot validate motor direction, servo travel, slide geometry, shooter readiness, or camera reliability.
