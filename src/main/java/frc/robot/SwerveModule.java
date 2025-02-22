package frc.robot;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Rotation2d;
// import edu.wpi.first.math.kinematics.ChassisSpeeds;
// import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.trajectory.ExponentialProfile.Constraints;
import edu.wpi.first.units.Units;
import edu.wpi.first.wpilibj.SPI;
//import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
// import edu.wpi.first.wpilibj.XboxController;


import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkAbsoluteEncoder;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkBaseConfig;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.ctre.phoenix6.hardware.CANcoder;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.SparkBase.ResetMode;


public class SwerveModule
{
    // Constants of physical Robot]\[]
    public static final double kAzimuthGearing = 150 / 7;
    public static final double kDriveGearing = 6.12f;
    public static final double kDriveCircumference = 0.31919f;


    // private Rotation2d offset; //offset is in radians and isnt even used rn
    //TrapezoidProfile.Constraints constraints = new TrapezoidProfile.Constraints(3 * 180, 5 * 180);
    
    private SparkMax AzimuthMotor;

    private CANcoder AbsoluteEncoder;

    private SparkMax DriveMotor;
    
    private ProfiledPIDController AzimuthPID = new ProfiledPIDController(0.08, 0, 0.0005, new TrapezoidProfile.Constraints(3 * 180, 5 * 180));
    //new PIDController(0.08,0,0.0005);
    
                                                                                                                                                                                                                                                                                                                                                                                                                                        
    

    

    private SlewRateLimiter DriveRateLimiter = new SlewRateLimiter(6);

    public SwerveModule(int azimuthMotorDeviceId, int driveMotorDeviceId, int encoderDeviceId)
    {
        this.AzimuthMotor = new SparkMax(azimuthMotorDeviceId, MotorType.kBrushless);
        // this.AzimuthMotor.setIdleMode(IdleMode.kCoast);
        // this.AzimuthMotor.setInverted(true);

        this.AbsoluteEncoder = new CANcoder(encoderDeviceId);
        // this.AzimuthMotor.getAlternateEncoder()

        // this.offset = azimuthOffset;

        this.DriveMotor = new SparkMax(driveMotorDeviceId, MotorType.kBrushless);
        this.DriveMotor.configure(new SparkMaxConfig().idleMode(IdleMode.kBrake), ResetMode.kNoResetSafeParameters, PersistMode.kPersistParameters);
     //   this.DriveMotor.setIdleMode(IdleMode.kCoast);

       this.AzimuthPID.enableContinuousInput(0, 360);
        this.AzimuthPID.setTolerance(2);
    }

    public Rotation2d getAzimuthRotation()
    {
      return Rotation2d.fromDegrees(this.AbsoluteEncoder.getAbsolutePosition().getValue().in(Units.Degrees));
    }
  
    /**
     * @return Velocity in meters per minute.
     */
   public double getDriveVelocity()
    {
      return this.DriveMotor.getEncoder().getVelocity() / kDriveGearing * kDriveCircumference;
    }

    public SwerveModulePosition getPosition()
    {
      double distanceInMeters = this.DriveMotor.getEncoder().getPosition() / kDriveGearing * kDriveCircumference;

      return new SwerveModulePosition(distanceInMeters, this.getAzimuthRotation());
    }

    public void Stop()
    {
        this.AzimuthMotor.stopMotor();
        this.DriveMotor.stopMotor();
        this.DriveRateLimiter.reset(0);
        this.AzimuthPID.setGoal(this.getAzimuthRotation().getDegrees());
    }

    public void setDesiredState(SwerveModuleState state) {
        // state.optimize();
        // state = SwerveModuleState.optimize(state, getAzimuthRotation());

        // SmartDashboard.putNumber("%d %d ".format("", null))

        double error = this.AzimuthPID.calculate(this.getAzimuthRotation().getDegrees(), state.angle.getDegrees());
        
        double controlVoltage = error * 0.95137420707;

        this.AzimuthMotor.setVoltage(controlVoltage);

        SmartDashboard.putNumber("MK4iSwerveModule Azimuth Control", error);
        SmartDashboard.putNumber("MK4iSwerveModule Azimuth Output Voltage", controlVoltage);

        double targetDriveSpeed = this.DriveRateLimiter.calculate(state.speedMetersPerSecond);

        // What is this number? - Aaron][\
        
        double driveVoltage = targetDriveSpeed * 2.88f;

        this.DriveMotor.setVoltage(driveVoltage);

        SmartDashboard.putNumber("MK4iSwerveModule Drive Control", state.speedMetersPerSecond);
        SmartDashboard.putNumber("MK4iSwerveModule Drive Voltage", driveVoltage);
    }

    public SwerveModuleState getMeasuredState()
    {
        return new SwerveModuleState(this.getDriveVelocity() / 60, this.getAzimuthRotation());
    }

    public SwerveModuleState getDesiredState() {
      return new SwerveModuleState(this.getSpeed(), this.getAzimuthRotation());
    }
    
    public double getSpeed() {
      return this.DriveMotor.getEncoder().getVelocity()/60/kDriveGearing*kDriveCircumference;
    }
}