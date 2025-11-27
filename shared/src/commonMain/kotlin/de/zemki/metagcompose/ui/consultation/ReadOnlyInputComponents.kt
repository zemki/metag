package de.zemki.metagcompose.ui.consultation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.zemki.metagcompose.data.model.InputFieldType
import de.zemki.metagcompose.data.model.ParsedInputField
import de.zemki.metagcompose.ui.theme.MetagColors

@Composable
fun InputTypeChip(inputType: InputFieldType) {
    val (icon, text, color) = when (inputType) {
        InputFieldType.TEXT -> Triple(Icons.Default.Edit, "Text", MetagColors.Primary)
        InputFieldType.ONE_CHOICE -> Triple(Icons.Default.Done, "Choice", Color(0xFF4CAF50))
        InputFieldType.MULTIPLE_CHOICE -> Triple(Icons.Default.Check, "Multiple", Color(0xFF2196F3))
        InputFieldType.SCALE -> Triple(Icons.Default.Star, "Scale", Color(0xFFFF9800))
        InputFieldType.AUDIO_RECORDING -> Triple(Icons.Default.PlayArrow, "Audio", Color(0xFF9C27B0))
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ReadOnlyTextField(inputField: ParsedInputField) {
    val value = inputField.getDisplayValue()
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "No text entered",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun ReadOnlyOneChoice(inputField: ParsedInputField) {
    val selectedValue = inputField.getDisplayValue()
    val availableOptions = inputField.definition.getAvailableOptions()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Selected value display
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MetagColors.Primary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, MetagColors.Primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    tint = MetagColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = selectedValue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MetagColors.Primary
                )
            }
        }
        
        // Available options (dimmed)
        if (availableOptions.isNotEmpty()) {
            Text(
                text = "Available options:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableOptions) { option ->
                    val isSelected = option == selectedValue.replace("Not selected", "")
                    
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) {
                            MetagColors.Primary.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                        border = if (isSelected) {
                            BorderStroke(1.dp, MetagColors.Primary.copy(alpha = 0.3f))
                        } else null
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) {
                                MetagColors.Primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadOnlyMultipleChoice(inputField: ParsedInputField) {
    val selectedOptions = inputField.getSelectedOptions()
    val availableOptions = inputField.definition.getAvailableOptions()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedOptions.isNotEmpty()) {
            Text(
                text = "Selected options:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedOptions) { option ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MetagColors.Primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MetagColors.Primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MetagColors.Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MetagColors.Primary
                            )
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "No options selected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
        
        // All available options (dimmed)
        if (availableOptions.isNotEmpty()) {
            Text(
                text = "All options:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(availableOptions) { option ->
                    val isSelected = selectedOptions.contains(option)
                    
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) {
                            MetagColors.Primary.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                        border = if (isSelected) {
                            BorderStroke(1.dp, MetagColors.Primary.copy(alpha = 0.3f))
                        } else null
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) {
                                MetagColors.Primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadOnlyScale(inputField: ParsedInputField) {
    val scaleValue = inputField.getScaleValue()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scale value display
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MetagColors.Primary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, MetagColors.Primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MetagColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Rating",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (scaleValue != null && scaleValue in 1..5) {
                    Text(
                        text = "$scaleValue/5",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MetagColors.Primary
                    )
                } else {
                    Text(
                        text = "Not rated",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
        
        // Visual scale display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(5) { index ->
                val value = index + 1
                val isSelected = scaleValue == value
                val isBeforeSelected = scaleValue != null && value <= scaleValue
                
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = when {
                        isSelected -> MetagColors.Primary
                        isBeforeSelected -> MetagColors.Primary.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        1.dp, 
                        if (isBeforeSelected) MetagColors.Primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> Color.White
                                isBeforeSelected -> MetagColors.Primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadOnlyAudioField(
    inputField: ParsedInputField,
    entriesRepository: de.zemki.metagcompose.data.repository.EntriesRepository
) {
    val hasAudio = inputField.hasAudioFile()
    val fileId = inputField.getAudioFileId()
    
    if (hasAudio && fileId != null) {
        // Show functional audio player
        AudioPlayerComponent(
            fileId = fileId,
            entriesRepository = entriesRepository
        )
    } else {
        // Show placeholder for no audio
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "No Audio Recording",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No audio file attached",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}