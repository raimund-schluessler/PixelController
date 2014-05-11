/**
 * Copyright (C) 2011-2013 Michael Vogt <michu@neophob.com>
 *
 * This file is part of PixelController.
 *
 * PixelController is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PixelController is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PixelController.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.neophob.sematrix.core.output;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.neophob.sematrix.core.output.tinkerforge.IPConnection;
import com.neophob.sematrix.core.output.tinkerforge.BrickletLEDStrip;
import com.neophob.sematrix.core.properties.ApplicationConfigurationHelper;

/**
 * does nothin.
 *
 * @author michu
 */
public class TinkerforgeDevice extends Output {

    /** The log. */
    private static final Logger LOG = Logger.getLogger(PixelInvadersNetDevice.class.getName());

    protected Map<Integer, Object> transformedBuffer = new HashMap<Integer, Object>(); 

    protected boolean initialized = false;

    private static int ledIndex = 0;
    private static short[] r = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static short[] g = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static short[] b = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static int nrLeds = 144;

    /** The Tinkerforge Device. */
    private BrickletLEDStrip ledStrip = null;
    private IPConnection ipcon = null;

    /**
     * init Tinkerforge LED Bricklet.
     *
     * @param controller the controller
     */
    public TinkerforgeDevice(ApplicationConfigurationHelper ph) {
        super(OutputDeviceEnum.TINKERFORGE, ph, 8);
        this.supportConnectionState = true;

        String ip = ph.getTinkerforgeIp();
        int port = ph.getTinkerforgePort();
        String UID = ph.getTinkerforgeUid();
        try {
            ipcon = new IPConnection(); // Create IP connection
            ledStrip = new BrickletLEDStrip(UID, ipcon); // Create device object

            ipcon.connect(ip, port); // Connect to brickd
            if (ipcon.getConnectionState() == 1) {
                this.initialized = true;
            }                       
        } catch (Exception e) {
            LOG.log(Level.WARNING, "failed to conect to Tinkerforge LED Bricklet network device!", e);
        }

    }

    /* (non-Javadoc)
     * @see com.neophob.sematrix.core.output.Output#update()
     */
    public void update() {
        if (initialized) {
            sendPayload();      
        }
    }

    /**
     * 
     * @param 
     * @param 
     */
    public void sendPayload() {
        int[] bfr = super.getBufferForScreen(1);
        bfr = OutputHelper.flipSecondScanline(bfr, 12, 12);

        this.transformedBuffer.put(1, bfr);

        int tmp;
        int numLeds = nrLeds;
        int leds = 0;
        int index = 0;
        int led = 0;
        for (Map.Entry<Integer, Object> entry: this.transformedBuffer.entrySet()) {
            int[] data = (int[])entry.getValue();
            while (numLeds>0){
                numLeds = numLeds - 16;
                if (numLeds < 0){
                    leds = 16 + numLeds;
                } else{
                    leds = 16;
                }
                for (int n=0; n<16; n++) {
                    tmp = data[led];
                    r[n] = (short) ((tmp>>16) & 255);
                    g[n] = (short) ((tmp>>8)  & 255);
                    b[n] = (short) ( tmp      & 255);
                    led++;
                }

                try{
                    ledStrip.setRGBValues(index, (short)16, r, b, g);
                } catch (Exception e){
                    LOG.log(Level.WARNING, "", e);
                }
                index = index + leds;
            }
        }
        
    }

    /* (non-Javadoc)
     * @see com.neophob.sematrix.core.output.Output#close()
     */
    @Override
    public void close() {
        if (initialized) {
            try {
                ipcon.disconnect();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Device is not connected. Cannot disconnect.", e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        if (initialized) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSupportConnectionState() {
        return true;
    }

    @Override
    public String getConnectionStatus() {
        int state = ipcon.getConnectionState();
        switch (state) {
        case 0:
            return "Not connected to Tinkerforge LED Device"; 
        case 1:
            return "Connected to Tinkerforge LED Device";          
        case 2:
            return "Trying to Connect to Tinkerforge LED Device.";
        default:
            return "Connection status unknown.";
        }
    }

}
