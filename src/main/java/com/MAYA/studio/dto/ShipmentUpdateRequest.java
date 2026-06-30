package com.MAYA.studio.dto;

import com.MAYA.studio.entity.Order;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShipmentUpdateRequest {
    private String courierName;
    private String trackingNumber;
    private LocalDateTime shipmentDate;
    private LocalDateTime expectedDelivery;
    private String shipmentNotes;
    private String customerShipmentNotes;
    private Order.OrderStatus status;
}
