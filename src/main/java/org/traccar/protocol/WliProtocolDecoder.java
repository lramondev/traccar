/*
 * Copyright 2020 - 2022 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class WliProtocolDecoder extends BaseProtocolDecoder {

    public WliProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.readUnsignedByte(); // header
        int clazz = buf.readUnsignedByte();

        if (clazz == '1') {

            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession == null) {
                return null;
            }

            Position position = new Position(getProtocolName());
            position.setRastreador_id(deviceSession.getDeviceId());

            CellTower cellTower = new CellTower();

            position.set(Position.KEY_INDEX, buf.readUnsignedShort());

            buf.readUnsignedShort(); // length
            buf.readUnsignedShort(); // checksum
            int type = buf.readUnsignedByte();
            buf.readUnsignedByte(); // delimiter

            while (buf.readableBytes() > 1) {

                int fieldNumber = buf.readUnsignedByte();

                buf.readUnsignedByte(); // delimiter

                if (buf.getUnsignedByte(buf.readerIndex()) == 0xFF) {

                    buf.readUnsignedByte(); // binary type indication
                    int endIndex = buf.readUnsignedShort() + buf.readerIndex();

                    if (fieldNumber == 52) {
                        position.setValido(true);
                        buf.readUnsignedByte(); // reason
                        buf.readUnsignedByte(); // century
                        DateBuilder dateBuilder = new DateBuilder()
                                .setDate(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte())
                                .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                        position.setDatahora_calculada(dateBuilder.getDate());
                        position.setLatitude(buf.readInt() / 600000.0);
                        position.setLongitude(buf.readInt() / 600000.0);
                        position.setVelocidade(buf.readUnsignedShort());
                        position.setCurso(buf.readUnsignedShort() * 0.1);
                        position.set(Position.KEY_ODOMETER, UnitsConverter.metersFromFeet(buf.readUnsignedInt()));
                        position.setAltitude(buf.readInt() * 0.1);
                    }

                    buf.readerIndex(endIndex);

                } else {

                    int endIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 0);
                    String value = buf.readCharSequence(
                            endIndex - buf.readerIndex(), StandardCharsets.US_ASCII).toString();

                    int networkFieldsOffset;
                    switch (type) {
                        case 0xE4:
                            networkFieldsOffset = 10;
                            break;
                        case 0xCB:
                            networkFieldsOffset = 80;
                            break;
                        case 0x1E:
                            networkFieldsOffset = 182;
                            break;
                        case 0xC9:
                        default:
                            networkFieldsOffset = 35;
                            break;
                    }
                    if (fieldNumber - networkFieldsOffset >= 0 && fieldNumber - networkFieldsOffset < 10) {
                        switch (fieldNumber - networkFieldsOffset) {
                            case 0:
                                cellTower.setMobileCountryCode(Integer.parseInt(value));
                                break;
                            case 1:
                                cellTower.setMobileNetworkCode(Integer.parseInt(value));
                                break;
                            case 2:
                                cellTower.setLocationAreaCode(Integer.parseInt(value));
                                break;
                            case 3:
                                cellTower.setCellId(Long.parseLong(value));
                                break;
                            case 4:
                                cellTower.setSignalStrength(Integer.parseInt(value));
                                break;
                            default:
                                break;
                        }
                    } else {
                        switch (fieldNumber) {
                            case 246:
                                String[] values = value.split(",");
                                position.set(Position.KEY_POWER, Integer.parseInt(values[2]) * 0.01);
                                position.set(Position.KEY_BATTERY, Integer.parseInt(values[3]) * 0.01);
                                break;
                            case 255:
                                position.setDatahora_rastreador(new Date(Long.parseLong(value) * 1000));
                                break;
                            default:
                                break;
                        }
                    }

                }

                buf.readUnsignedByte(); // delimiter

            }

            if (type == 0xE4) {
                getLastLocation(position, position.getDatahora_rastreador());
            }

            if (cellTower.getCellId() != null) {
                position.setRede(new Network(cellTower));
            }

            if (!position.getValido()) {
                getLastLocation(position, position.getDatahora_rastreador());
            }

            return position;

        } else if (clazz == '2') {

            String id = buf.toString(buf.readerIndex(), buf.readableBytes() - 1, StandardCharsets.US_ASCII);
            getDeviceSession(channel, remoteAddress, id.substring("wli:".length()));
            return null;

        }

        return null;
    }

}
