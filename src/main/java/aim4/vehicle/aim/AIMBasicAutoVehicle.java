/*
Copyright (c) 2011 Tsz-Chiu Au, Peter Stone
University of Texas at Austin
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the University of Texas at Austin nor the names of its
contributors may be used to endorse or promote products derived from this
software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package aim4.vehicle.aim;

import aim4.config.Debug;
import aim4.driver.Driver;
import aim4.driver.aim.AIMAutoDriver;
import aim4.msg.aim.i2v.I2VMessage;
import aim4.msg.aim.v2i.V2IMessage;
import aim4.vehicle.BasicAutoVehicle;
import aim4.vehicle.VehicleSpec;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * The basic autonomous vehicle.
 */
public class AIMBasicAutoVehicle extends BasicAutoVehicle
        implements AIMAutoVehicleSimModel {

    /**
     * The default distance the Vehicle can transmit messages.
     * {@value} meters.
     */
    public static final double DEFAULT_TRANSMISSION_POWER = 250; // meters

    /////////////////////////////////
    // PRIVATE FIELDS
    /////////////////////////////////

    /**
     * The Driver controlling this vehicle.
     */
    protected AIMAutoDriver driver;

    /**
     * The maximum distance the Vehicle can transmit a message, in meters.
     */
    private double transmissionPower = DEFAULT_TRANSMISSION_POWER;


    /////////////////////////////////
    // PRIVATE FIELDS
    /////////////////////////////////

    // V2I Communications systems

    /**
     * The messages waiting to be sent from the Vehicle to an
     * IntersectionManager.
     */
    private Queue<V2IMessage> v2iOutbox = new LinkedList<V2IMessage>();

    /**
     * The messages waiting to be received from an IntersectionManager and
     * processed by the Vehicle.
     */
    private Queue<I2VMessage> i2vInbox = new LinkedList<I2VMessage>();


    // Stats on communication

    /** The number of bits this Vehicle has received. */
    protected int bitsReceived;
    // TODO: change protected to private after figuring out where should
    // the proxy vehicle put.

    /** The number of bits this Vehicle has transmitted. */
    private int bitsTransmitted;

    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // Note: these should be changed to Vector if they need to be synchronized
    //       because ArrayList is not thread-safe, whereas Vector is. Another
    //       option would be to use
    //             Collections.synchronizedList(new ArrayList(...));
    //
    //       These are using an ArrayList instead of a LinkedList because we
    //       plan to do a lot of adding and removing, and while this might be
    //       faster with a LinkedList were we to delete items off the front
    //       one at a time, this causes a lot of memory allocation/cleanup.
    //       Instead, we use an ArrayList, process all messages at once, and
    //       then clear the whole list.  In this way, the memory that is
    //       allocated for the List sticks around and doesn't have to be
    //       reallocated every time new messages are added.
    //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    /////////////////////////////////
    // PRIVATE FIELDS
    /////////////////////////////////

    // debug

    /**
     * The last V2I message
     */
    private V2IMessage lastV2IMessage;

    //Result properties
    private double finishTime;
    private double delayTime;
    private double finalVelocity;
    private double maxVelocity;
    private double minVelocity;
    private double finalXPos;
    private double finalYPos;

    /////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////

    // TODO: reorganize the parameter order.

    /**
     * Construct a vehicle
     *
     * @param spec            the vehicle's specification
     * @param pos             the initial position of the Vehicle
     * @param heading         the initial heading of the Vehicle
     * @param steeringAngle   the initial steering angle of the Vehicle
     * @param velocity        the initial velocity of the Vehicle
     * @param targetVelocity  the initial target velocity
     * @param acceleration    the initial acceleration of the Vehicle
     * @param currentTime     the current time
     */
    public AIMBasicAutoVehicle(VehicleSpec spec,
                               Point2D pos,
                               double heading,
                               double steeringAngle,
                               double velocity,
                               double targetVelocity,
                               double acceleration,
                               double currentTime) {
        super(spec, pos, heading, velocity, steeringAngle, acceleration,
                targetVelocity, currentTime);
    }

    /////////////////////////////////
    // PUBLIC METHODS
    /////////////////////////////////

    //Driver accessors

    /**
     * {@inheritDoc}
     */
    @Override
    public AIMAutoDriver getDriver() {
        return driver;
    }

    /**
     * {@inheritDoc}
     *
     * In this instance, forces Driver to be an AIMAutoDriver with an assert.
     *
     * @param driver  the new driver to control this Vehicle
     */
    @Override
    public void setDriver(Driver driver) {
        assert driver instanceof AIMAutoDriver;
        this.driver = (AIMAutoDriver) driver;
    }

    // messaging

    /**
     * Set the Vehicle's transmission power.
     *
     * @param transmissionPower the new transmission power, in meters
     */
    public void setTransmissionPower(double transmissionPower) {
        this.transmissionPower = transmissionPower;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTransmissionPower() {
        return transmissionPower;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Queue<V2IMessage> getV2IOutbox() {
        return v2iOutbox;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<I2VMessage> pollAllMessagesFromI2VInbox() {
        // TODO: many need to make this function atomic to avoid
        // putting messages in the queue and retrieve from it at the same time.
        List<I2VMessage> msgs = new ArrayList<I2VMessage>(i2vInbox);
        i2vInbox.clear();
        return msgs;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void send(V2IMessage msg) {
        if (Debug.isPrintVehicleOutboxMessageOfVIN(msg.getVin())) {
            System.err.printf("vin %d sends message: %s\n", vin, msg);
        }
        v2iOutbox.add(msg);
        bitsTransmitted += msg.getSize();
        lastV2IMessage = msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void receive(I2VMessage msg) {
        i2vInbox.add(msg);
        bitsReceived += msg.getSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBitsReceived() {
        return bitsReceived;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBitsTransmitted() {
        return bitsTransmitted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V2IMessage getLastV2IMessage() {
        return lastV2IMessage;
    }

    //results
    @Override
    public double getFinishTime() {
        return finishTime;
    }

    @Override
    public void setFinishTime(double finishTime) {
        this.finishTime = finishTime;
    }

    @Override
    public double getDelay() {
        return delayTime;
    }

    @Override
    public void setDelay(double delayTime) {
        this.delayTime = delayTime;
    }

    @Override
    public double getFinalVelocity() {
        return finalVelocity;
    }

    @Override
    public void setFinalVelocity(double finalVelocity) {
        this.finalVelocity = finalVelocity;
    }

    @Override
    public double getMaxVelocity() {
        return maxVelocity;
    }

    @Override
    public void setMaxVelocity(double maxVelocity) {
        this.maxVelocity = maxVelocity;
    }

    @Override
    public double getMinVelocity() {
        return minVelocity;
    }

    @Override
    public void setMinVelocity(double minVelocity) {
        this.minVelocity = minVelocity;
    }

    @Override
    public double getFinalXPos() {
        return finalXPos;
    }

    @Override
    public void setFinalXPos(double finalXPos) {
        this.finalXPos = finalXPos;
    }

    @Override
    public double getFinalYPos() {
        return finalYPos;
    }

    @Override
    public void setFinalYPos(double finalYPos) {
        this.finalYPos = finalYPos;
    }

}
