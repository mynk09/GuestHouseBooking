package com.gzone.guesthousebooking.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gzone.guesthousebooking.data.model.GuestRoom

@Composable
fun RoomSelectionDialog(
    rooms: List<GuestRoom>,
    selectedRooms: List<Int>,
    onRoomSelectionChange: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var tempSelectedRooms by remember { mutableStateOf(selectedRooms.toMutableList()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Rooms",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(rooms) { room ->
                        RoomCheckboxItem(
                            room = room,
                            isSelected = tempSelectedRooms.contains(room.number),
                            onSelectionChange = { selected ->
                                if (selected) {
                                    tempSelectedRooms.add(room.number)
                                } else {
                                    tempSelectedRooms.remove(room.number)
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onRoomSelectionChange(tempSelectedRooms.toList())
                        onConfirm()
                    }) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
fun RoomCheckboxItem(
    room: GuestRoom,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Room ${room.number} - ${room.type}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "₹${room.ratePerNight}/night • Capacity: ${room.capacity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = room.amenities,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChange
        )
    }
    Divider(modifier = Modifier.padding(vertical = 4.dp))
}