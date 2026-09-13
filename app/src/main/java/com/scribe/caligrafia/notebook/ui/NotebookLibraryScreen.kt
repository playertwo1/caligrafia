package com.scribe.caligrafia.notebook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.caligrafia.core.model.NotebookCoverStyle
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.Notebook
import com.scribe.caligrafia.ui.theme.ScribeBluePrimary
import com.scribe.caligrafia.ui.theme.ScribePaper
import com.scribe.caligrafia.ui.theme.ScribeSuccess
import com.scribe.caligrafia.ui.theme.ScribeSurfaceBorder
import com.scribe.caligrafia.ui.theme.ScribeSurfaceLight
import com.scribe.caligrafia.ui.theme.ScribeTextMuted
import com.scribe.caligrafia.ui.theme.ScribeTextPrimary
import com.scribe.caligrafia.ui.theme.ScribeTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela da Biblioteca de Cadernos (Fluxo 01 dos PNGs — 01-biblioteca-cadernos.png).
 *
 * Exibe a coleção de cadernos do usuário com capas estilizadas, status offline e
 * BottomSheet intuitivo para criação de novos cadernos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookLibraryScreen(
    notebooks: List<Notebook>,
    currentNotebookId: String?,
    onSelectNotebook: (Notebook) -> Unit,
    onCreateNotebook: (String, GuidelineConfig, String) -> Unit,
    onDeleteNotebook: (Notebook) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ScribePaper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScribePaper,
                    titleContentColor = ScribeTextPrimary
                ),
                title = {
                    Text(
                        text = "Biblioteca de cadernos",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ScribeTextPrimary
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Cabeçalho com título e botão "+ Novo caderno"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Seus cadernos",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = ScribeTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Organize sua prática e acompanhe sua evolução.",
                        fontSize = 13.sp,
                        color = ScribeTextSecondary
                    )
                }

                if (notebooks.isNotEmpty()) {
                    Button(
                        onClick = { showCreateSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ScribeBluePrimary),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Novo caderno", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (notebooks.isEmpty()) {
                // Estado Vazio Conforme Mockup (PNG 01 - Estado 1)
                EmptyNotebooksView(onCreateClick = { showCreateSheet = true })
            } else {
                // Lista de Cadernos (PNG 01 - Estado 3)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(notebooks, key = { it.id }) { nb ->
                        NotebookItemCard(
                            notebook = nb,
                            isSelected = nb.id == currentNotebookId,
                            onClick = { onSelectNotebook(nb) },
                            onDelete = { onDeleteNotebook(nb) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateNotebookBottomSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { name, config, coverStyle ->
                onCreateNotebook(name, config, coverStyle)
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showCreateSheet = false
                }
            }
        )
    }
}

@Composable
private fun EmptyNotebooksView(onCreateClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f),
            colors = CardDefaults.cardColors(containerColor = ScribeSurfaceLight),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ScribeSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEBF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = ScribeBluePrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Crie seu primeiro caderno",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = ScribeTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Comece sua jornada na caligrafia. Escolha um estilo, um tipo de papel e dê um nome para o seu caderno.",
                    fontSize = 14.sp,
                    color = ScribeTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onCreateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ScribeBluePrimary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(0.85f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Novo caderno", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cada traço é um progresso.",
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = ScribeTextMuted
                )
            }
        }
    }
}

@Composable
fun NotebookCoverThumbnail(
    title: String,
    coverStyle: NotebookCoverStyle,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(coverStyle.primaryColorHex)
    val secondaryColor = Color(coverStyle.secondaryColorHex)
    val accentColor = Color(coverStyle.accentColorHex)
    val spineColor = Color(coverStyle.spineColorHex)
    val textColor = Color(coverStyle.textColorHex)

    Box(
        modifier = modifier
            .size(width = 72.dp, height = 96.dp)
            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 3.dp, bottomStart = 3.dp))
            .background(
                Brush.horizontalGradient(
                    0.0f to spineColor,
                    0.18f to spineColor,
                    0.20f to Color(0x33000000), // Sombra suave de dobra da lombada
                    0.26f to primaryColor,
                    1.0f to secondaryColor
                )
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 3.dp, bottomStart = 3.dp)
            )
            .padding(start = 14.dp, top = 8.dp, end = 6.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Moldura interna clássica com detalhe em foil/emboss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(0.75.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title.take(14),
                    color = textColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 11.sp,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NotebookItemCard(
    notebook: Notebook,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd 'de' MMM. 'de' yyyy", Locale("pt", "BR")) }
    val formattedDate = remember(notebook.updatedAt) { dateFormat.format(Date(notebook.updatedAt)) }
    val coverStyle = notebook.coverStyleEnum

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScribeSurfaceLight),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) ScribeBluePrimary else ScribeSurfaceBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Capa estilizada autêntica do caderno (4 estilos clássicos)
            NotebookCoverThumbnail(
                title = notebook.title,
                coverStyle = coverStyle
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notebook.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = ScribeTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${coverStyle.title} • Pautado 52°",
                    fontSize = 12.sp,
                    color = ScribeTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ScribeSuccess,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Salvo no dispositivo",
                        fontSize = 11.sp,
                        color = ScribeSuccess,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${notebook.pageIds.size} páginas • $formattedDate",
                    fontSize = 11.sp,
                    color = ScribeTextMuted
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opções", tint = ScribeTextSecondary)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Excluir caderno", color = Color(0xFFDC2626)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNotebookBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (String, GuidelineConfig, String) -> Unit
) {
    var notebookName by remember { mutableStateOf("") }
    var selectedCoverStyle by remember { mutableStateOf(NotebookCoverStyle.PAPEL_ARTESANAL) }
    var selectedPaperType by remember { mutableStateOf("COPPERPLATE_52") } // COPPERPLATE_52, ESCOLAR, BRANCO

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ScribeSurfaceLight,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Criar novo caderno",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = ScribeTextPrimary
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text("Nome do caderno", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ScribeTextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = notebookName,
                onValueChange = { if (it.length <= 40) notebookName = it },
                placeholder = { Text("Ex: Estudos de Cursiva Inglesa", color = ScribeTextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ScribeBluePrimary,
                    unfocusedBorderColor = ScribeSurfaceBorder
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${notebookName.length}/40",
                fontSize = 11.sp,
                color = ScribeTextMuted,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Estilo da capa", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ScribeTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            // 4 estilos clássicos em grade 2x2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val styles = NotebookCoverStyle.entries
                for (row in styles.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (style in row) {
                            val isSelected = selectedCoverStyle == style
                            val swatchBg = Color(style.primaryColorHex)
                            val swatchBorder = Color(style.accentColorHex)

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCoverStyle = style },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFEFF6FF) else ScribeSurfaceLight
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ScribeBluePrimary else ScribeSurfaceBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Amostra de cor da capa
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(swatchBg)
                                            .border(1.dp, swatchBorder, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = style.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = ScribeTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Tipo de pauta", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ScribeTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Opção 1: Pautado 52° (Copperplate)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedPaperType = "COPPERPLATE_52" },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPaperType == "COPPERPLATE_52") Color(0xFFEFF6FF) else ScribeSurfaceLight
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selectedPaperType == "COPPERPLATE_52") 2.dp else 1.dp,
                        color = if (selectedPaperType == "COPPERPLATE_52") ScribeBluePrimary else ScribeSurfaceBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Pautado 52°", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = ScribeTextPrimary)
                        Text("Copperplate", fontSize = 10.sp, color = ScribeTextSecondary)
                    }
                }

                // Opção 2: Escolar
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedPaperType = "ESCOLAR" },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPaperType == "ESCOLAR") Color(0xFFEFF6FF) else ScribeSurfaceLight
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selectedPaperType == "ESCOLAR") 2.dp else 1.dp,
                        color = if (selectedPaperType == "ESCOLAR") ScribeBluePrimary else ScribeSurfaceBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Escolar", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = ScribeTextPrimary)
                        Text("1:1:1 padrão", fontSize = 10.sp, color = ScribeTextSecondary)
                    }
                }

                // Opção 3: Em branco
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedPaperType = "BRANCO" },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPaperType == "BRANCO") Color(0xFFEFF6FF) else ScribeSurfaceLight
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selectedPaperType == "BRANCO") 2.dp else 1.dp,
                        color = if (selectedPaperType == "BRANCO") ScribeBluePrimary else ScribeSurfaceBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Em branco", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = ScribeTextPrimary)
                        Text("Sem guias", fontSize = 10.sp, color = ScribeTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = ScribeTextSecondary)
                }

                Button(
                    onClick = {
                        val config = when (selectedPaperType) {
                            "COPPERPLATE_52" -> GuidelineConfig.copperplate()
                            "ESCOLAR" -> GuidelineConfig.school()
                            else -> GuidelineConfig.copperplate()
                        }
                        onCreate(
                            notebookName.ifBlank { "Caderno ${selectedCoverStyle.title}" },
                            config,
                            selectedCoverStyle.id
                        )
                    },
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = ScribeBluePrimary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Criar e abrir caderno", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}
