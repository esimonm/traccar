/*
 * Copyright 2015 - 2021 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.BcdUtil;
import org.traccar.helper.BitUtil;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class TzoneProtocolDecoder extends BaseProtocolDecoder {

    public TzoneProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private String decodeAlarm(Short value) {
        switch (value) {
            case 0x01:
                return Position.ALARM_SOS;
            case 0x10:
                return Position.ALARM_LOW_BATTERY;
            case 0x11:
                return Position.ALARM_OVERSPEED;
            case 0x14:
                return Position.ALARM_BRAKING;
            case 0x15:
                return Position.ALARM_ACCELERATION;
            case 0x30:
                return Position.ALARM_PARKING;
            case 0x42:
                return Position.ALARM_GEOFENCE_EXIT;
            case 0x43:
                return Position.ALARM_GEOFENCE_ENTER;
            default:
                return null;
        }
    }

    private boolean decodeGps(Position position, ByteBuf buf, int hardware) {

        int blockLength = buf.readUnsignedShort();
        int blockEnd = buf.readerIndex() + blockLength;

        if (blockLength < 22) {
            return false;
        }

        if (hardware == 0x413) {
            buf.readUnsignedByte(); // status
        } else {
            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        }

        if (hardware == 0x413) {
            position.setFixTime(new DateBuilder()
                    .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()).getDate());
        }

        double lat;
        double lon;

        if (hardware == 0x10A || hardware == 0x10B) {
            lat = buf.readUnsignedInt() / 600000.0;
            lon = buf.readUnsignedInt() / 600000.0;
        } else {
            lat = buf.readUnsignedInt() / 100000.0 / 60.0;
            lon = buf.readUnsignedInt() / 100000.0 / 60.0;
        }

        if (hardware == 0x413) {

            position.set(Position.KEY_HDOP, buf.readUnsignedShort() * 0.1);

            position.setAltitude(buf.readUnsignedShort());
            position.setCourse(buf.readUnsignedShort());
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() * 0.1));

            position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());

        } else {

            position.setFixTime(new DateBuilder()
                    .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                    .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()).getDate());

            position.setSpeed(buf.readUnsignedShort() * 0.01);

            position.set(Position.KEY_ODOMETER, buf.readUnsignedMedium());

            int flags = buf.readUnsignedShort();
            position.setCourse(BitUtil.to(flags, 9));
            if (!BitUtil.check(flags, 10)) {
                lat = -lat;
            }
            position.setLatitude(lat);
            if (BitUtil.check(flags, 9)) {
                lon = -lon;
            }
            position.setLongitude(lon);
            position.setValid(BitUtil.check(flags, 11));

        }

        buf.readerIndex(blockEnd);

        return true;
    }

    private void decodeCards(Position position, ByteBuf buf) {

        int index = 1;
        for (int i = 0; i < 4; i++) {

            int blockLength = buf.readUnsignedShort();
            int blockEnd = buf.readerIndex() + blockLength;

            if (blockLength > 0) {

                int count = buf.readUnsignedByte();
                for (int j = 0; j < count; j++) {

                    int length = buf.readUnsignedByte();

                    boolean odd = length % 2 != 0;
                    if (odd) {
                        length += 1;
                    }

                    String num = ByteBufUtil.hexDump(buf.readSlice(length / 2));

                    if (odd) {
                        num = num.substring(1);
                    }

                    position.set("card" + index, num);
                }
            }

            buf.readerIndex(blockEnd);
        }

    }

    private void decodePassengers(Position position, ByteBuf buf) {

        int blockLength = buf.readUnsignedShort();
        int blockEnd = buf.readerIndex() + blockLength;

        if (blockLength > 0) {

            position.set("passengersOn", buf.readUnsignedMedium());
            position.set("passengersOff", buf.readUnsignedMedium());

        }

        buf.readerIndex(blockEnd);

    }

    private void decodeTags(Position position, ByteBuf buf) {

        int blockLength = buf.readUnsignedShort();
        int blockEnd = buf.readerIndex() + blockLength;

        if (blockLength > 0) {

            buf.readUnsignedByte(); // tag type

            int count = buf.readUnsignedByte();
            int tagLength = buf.readUnsignedByte();

            for (int i = 1; i <= count; i++) {
                int tagEnd = buf.readerIndex() + tagLength;

                buf.readUnsignedByte(); // status
                buf.readUnsignedShortLE(); // battery voltage

                position.set(Position.PREFIX_TEMP + i, (buf.readShortLE() & 0x3fff) * 0.1);

                buf.readUnsignedByte(); // humidity
                buf.readUnsignedByte(); // rssi

                buf.readerIndex(tagEnd);
            }

        }

        buf.readerIndex(blockEnd);

    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // header
        buf.readUnsignedShort(); // length
        if (buf.readUnsignedShort() != 0x2424) {
            return null;
        }
        int hardware = buf.readUnsignedShort();
        long firmware = buf.readUnsignedInt();

        String imei = ByteBufUtil.hexDump(buf.readSlice(8)).substring(1);
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.set(Position.KEY_VERSION_HW, hardware);
        position.set(Position.KEY_VERSION_FW, firmware);

        position.setDeviceTime(new DateBuilder()
                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte()).getDate());

        // GPS info

        if (hardware == 0x406 || !decodeGps(position, buf, hardware)) {

            getLastLocation(position, position.getDeviceTime());

        }

        // LBS info

        int blockLength = buf.readUnsignedShort();
        int blockEnd = buf.readerIndex() + blockLength;

        if (blockLength > 0) {
            if (hardware == 0x10A || hardware == 0x10B || hardware == 0x406) {

                position.setNetwork(new Network(
                        CellTower.fromLacCid(buf.readUnsignedShort(), buf.readUnsignedShort())));

            } else if (hardware == 0x407) {

                Network network = new Network();
                int count = buf.readUnsignedByte();
                for (int i = 0; i < count; i++) {
                    buf.readUnsignedByte(); // signal information

                    int mcc = BcdUtil.readInteger(buf, 4);
                    int mnc = BcdUtil.readInteger(buf, 4) % 1000;

                    network.addCellTower(CellTower.from(
                            mcc, mnc, buf.readUnsignedShort(), buf.readUnsignedInt()));
                }
                position.setNetwork(network);

            }
        }

        buf.readerIndex(blockEnd);

        // Status info

        blockLength = buf.readUnsignedShort();
        blockEnd = buf.readerIndex() + blockLength;

        if (blockLength >= 13) {
            position.set(Position.KEY_ALARM, decodeAlarm(buf.readUnsignedByte()));
            position.set("terminalInfo", buf.readUnsignedByte());

            if (hardware != 0x407) {
                int status = buf.readUnsignedByte();
                position.set(Position.PREFIX_OUT + 1, BitUtil.check(status, 0));
                position.set(Position.PREFIX_OUT + 2, BitUtil.check(status, 1));
                status = buf.readUnsignedByte();
                position.set(Position.PREFIX_IN + 1, BitUtil.check(status, 4));
                if (BitUtil.check(status, 0)) {
                    position.set(Position.KEY_ALARM, Position.ALARM_SOS);
                }
            }

            position.set(Position.KEY_RSSI, buf.readUnsignedByte());
            position.set("gsmStatus", buf.readUnsignedByte());
            position.set(Position.KEY_BATTERY, buf.readUnsignedShort());

            if (hardware != 0x407) {
                position.set(Position.KEY_POWER, buf.readUnsignedShort());
                position.set(Position.PREFIX_ADC + 1, buf.readUnsignedShort());
                position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
            } else {
                position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedShort());
                position.set("humidity", buf.readUnsignedShort());
                position.set("lightSensor", buf.readUnsignedByte());
            }
        }

        if (blockLength >= 15) {
            position.set(Position.PREFIX_TEMP + 1, buf.readUnsignedShort());
        }

        buf.readerIndex(blockEnd);

        if (hardware == 0x10B) {

            decodeCards(position, buf);

            buf.skipBytes(buf.readUnsignedShort()); // temperature
            buf.skipBytes(buf.readUnsignedShort()); // lock

            decodePassengers(position, buf);

        }

        if (hardware == 0x406) {

            decodeTags(position, buf);

        }

        return position;
    }

}
