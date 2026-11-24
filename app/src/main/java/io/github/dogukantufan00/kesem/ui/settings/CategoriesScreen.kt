package io.github.dogukantufan00.kesem.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.data.local.entity.CategoryEntity
import io.github.dogukantufan00.kesem.model.CategoryType
import io.github.dogukantufan00.kesem.ui.components.EmptyStateCard
import io.github.dogukantufan00.kesem.ui.components.KesemCard
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.ui.theme.adaptiveAccent
import io.github.dogukantufan00.kesem.ui.theme.adaptiveBadgeBackground
import io.github.dogukantufan00.kesem.util.CategoryIconRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onSaveCategory: (editingId: Long?, name: String, type: CategoryType, iconKey: String, colorHex: Long) -> Unit,
    onDeleteCategory: (Long) -> Unit
) {
    val kesemColors = LocalKesemColors.current
    var selectedTab by remember { mutableStateOf(CategoryType.EXPENSE) }
    var showFormDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    val filteredCategories = categories.filter { it.type == selectedTab }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Kategoriler",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingCategory = null
                            showFormDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Yeni Kategori"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val tabs = listOf(CategoryType.INCOME to "Gelir", CategoryType.EXPENSE to "Gider")
                    tabs.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = selectedTab == type,
                            onClick = { selectedTab = type },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tabs.size
                            )
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            if (filteredCategories.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Category,
                        title = "Kategori yok",
                        subtitle = "Bu tür için yeni bir kategori ekleyebilirsin.",
                        actionLabel = "Kategori Ekle",
                        onAction = {
                            editingCategory = null
                            showFormDialog = true
                        }
                    )
                }
            } else {
                item {
                    KesemCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        filteredCategories.forEachIndexed { index, cat ->
                            val rawColor = Color(cat.colorHex)
                            val tint = rawColor.adaptiveAccent(kesemColors.isDark)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(rawColor.adaptiveBadgeBackground(kesemColors.isDark)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIconRegistry.iconFor(cat.iconKey),
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        editingCategory = cat
                                        showFormDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Düzenle",
                                        tint = kesemColors.subtleText,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { deletingCategory = cat }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Sil",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            if (index < filteredCategories.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = kesemColors.cardBorder
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        CategoryFormDialog(
            initialCategory = editingCategory,
            defaultType = selectedTab,
            onDismiss = { showFormDialog = false },
            onSave = { name, type, iconKey, colorHex ->
                onSaveCategory(editingCategory?.id, name, type, iconKey, colorHex)
                showFormDialog = false
            }
        )
    }

    if (deletingCategory != null) {
        val cat = deletingCategory!!
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Kategoriyi Sil") },
            text = {
                Text("\"${cat.name}\" kategorisi silinecek. Bu kategoriye bağlı eski işlemler silinmez, \"Kategorisiz\" olarak kalır.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(cat.id)
                        deletingCategory = null
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text("İptal")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryFormDialog(
    initialCategory: CategoryEntity?,
    defaultType: CategoryType,
    onDismiss: () -> Unit,
    onSave: (name: String, type: CategoryType, iconKey: String, colorHex: Long) -> Unit
) {
    var name by remember(initialCategory) { mutableStateOf(initialCategory?.name ?: "") }
    val type = initialCategory?.type ?: defaultType
    var iconKey by remember(initialCategory) {
        mutableStateOf(initialCategory?.iconKey ?: CategoryIconRegistry.options.first().key)
    }
    var colorHex by remember(initialCategory) {
        mutableLongStateOf(initialCategory?.colorHex ?: CategoryIconRegistry.colorPalette.first())
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCategory == null) "Yeni Kategori" else "Kategoriyi Düzenle"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Kategori Adı") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Renk",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalKesemColors.current.subtleText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CategoryIconRegistry.colorPalette.forEach { hex ->
                        val selected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .then(
                                    if (selected) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "İkon",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalKesemColors.current.subtleText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconRegistry.options.forEach { option ->
                        val selected = iconKey == option.key
                        val rawColor = Color(colorHex)
                        val isDark = LocalKesemColors.current.isDark
                        val activeColor = rawColor.adaptiveAccent(isDark)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) {
                                        rawColor.adaptiveBadgeBackground(isDark)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .then(
                                    if (selected) {
                                        Modifier.border(1.5.dp, activeColor, RoundedCornerShape(10.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { iconKey = option.key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        error = "Kategori adı boş olamaz."
                    } else {
                        onSave(trimmed, type, iconKey, colorHex)
                    }
                }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
