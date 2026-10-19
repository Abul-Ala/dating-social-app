package com.datingsocial.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.datingsocial.app.ui.theme.*

@Composable
fun ReportBlockDialog(
    targetUserName: String,
    onDismiss: () -> Unit,
    onBlockOnly: (String) -> Unit,
    onReportAndBlock: (String, String) -> Unit
) {
    val reasons = listOf(
        "Inappropriate messages or photos",
        "Suspected fake profile or scam",
        "Harassment or offensive behavior",
        "Underage user",
        "Spam or advertising"
    )

    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var additionalDetails by remember { mutableStateOf("") }
    var showReportForm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PrimaryPink,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Safety & Moderation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "What would you like to do regarding $targetUserName?",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                if (!showReportForm) {
                    Button(
                        onClick = { onBlockOnly("User block") },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = PassRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Block $targetUserName", color = PassRed, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { showReportForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Report, contentDescription = null, tint = PrimaryPink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report & Block Account", color = PrimaryPink, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text(
                        text = "Select a reason:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )

                    reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedReason = reason }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryPink)
                            )
                            Text(
                                text = reason,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = additionalDetails,
                        onValueChange = { additionalDetails = it },
                        placeholder = { Text("Additional details (optional)...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPink,
                            unfocusedBorderColor = SurfaceCardLight,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Button(
                        onClick = { onReportAndBlock(selectedReason, additionalDetails) },
                        colors = ButtonDefaults.buttonColors(containerColor = PassRed),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit Report & Block", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    }
}
