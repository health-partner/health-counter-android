package doyoung.practice.healthcounternew.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import java.util.*
import kotlin.collections.ArrayList

class BluetoothUtils {
    companion object {
        const val SERVICE_STRING = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
        const val CHARACTERISTIC_COMMAND_STRING = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
        const val CHARACTERISTIC_RESPONSE_STRING = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

        fun findBLECharacteristics(gatt: BluetoothGatt): List<BluetoothGattCharacteristic> {
            val matchingCharacteristics: MutableList<BluetoothGattCharacteristic> = ArrayList()
            val serviceList = gatt.services
            val service = findGattService(serviceList) ?: return matchingCharacteristics
            val characteristicList = service.characteristics
            for (characteristic in characteristicList) {
                if (isMatchingCharacteristic(characteristic)) {
                    matchingCharacteristics.add(characteristic)
                }
            }
            return matchingCharacteristics
        }

        fun findCommandCharacteristic(gatt: BluetoothGatt): BluetoothGattCharacteristic? {
            return findCharacteristic(gatt, CHARACTERISTIC_COMMAND_STRING)
        }

        fun findResponseCharacteristic(gatt: BluetoothGatt): BluetoothGattCharacteristic? {
            val serviceUuid = UUID.fromString(SERVICE_STRING)
            val characteristicUuid = UUID.fromString(CHARACTERISTIC_RESPONSE_STRING)

            val service = gatt.getService(serviceUuid)
            return service?.getCharacteristic(characteristicUuid)
        }

        private fun findCharacteristic(
            gatt: BluetoothGatt,
            uuidString: String
        ): BluetoothGattCharacteristic? {
            val serviceList = gatt.services
            println("발견된 서비스 리스트: ${serviceList}")
            val service = findGattService(serviceList) ?: return null
            println("가라!!!!: ${service}")
            val characteristicList = service.characteristics
            for (characteristic in characteristicList) {
                if (matchCharacteristic(characteristic, uuidString)) {
                    return characteristic
                }
            }
            return null
        }

        private fun matchCharacteristic(
            characteristic: BluetoothGattCharacteristic?,
            uuidString: String
        ): Boolean {
            if (characteristic == null) {
                return false
            }
            val uuid: UUID = characteristic.uuid
            return matchUUIDs(uuid.toString(), uuidString)
        }

        private fun findGattService(serviceList: List<BluetoothGattService>): BluetoothGattService? {
            println("찾을 리스트 내부: ${serviceList}")
            for (service in serviceList) {
                val serviceUuidString = service.uuid.toString()
                if (matchServiceUUIDString(serviceUuidString)) {
                    return service
                }
            }
            return null
        }

        private fun matchServiceUUIDString(serviceUuidString: String): Boolean {
            return matchUUIDs(serviceUuidString, SERVICE_STRING)
        }

        private fun isMatchingCharacteristic(characteristic: BluetoothGattCharacteristic?): Boolean {
            if (characteristic == null) {
                return false
            }
            val uuid: UUID = characteristic.uuid
            return matchCharacteristicUUID(uuid.toString())
        }

        private fun matchCharacteristicUUID(characteristicUuidString: String): Boolean {
            return matchUUIDs(
                characteristicUuidString,
                CHARACTERISTIC_COMMAND_STRING,
                CHARACTERISTIC_RESPONSE_STRING
            )
        }

        private fun matchUUIDs(uuidString: String, vararg matches: String): Boolean {
            for (match in matches) {
                if (uuidString.equals(match, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }
}