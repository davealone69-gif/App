package com.example.appmaker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appmaker.data.ChatMessage
import com.example.appmaker.data.Conversation

@Composable
fun UnfilteredChatScreen(
    chatViewModel: ChatViewModel = viewModel(),
    onBack: () -> Unit
) {
    val conversations by chatViewModel.conversations.collectAsState()
    val currentConversationId by chatViewModel.currentConversationId.collectAsState()
    val messages by chatViewModel.currentMessages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val errorMessage by chatViewModel.errorMessage.collectAsState()
    
    var showNewChatDialog by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }
    var showSidebar by remember { mutableStateOf(true) }
    
    LaunchedEffect(messages.isNotEmpty()) {
        // Auto-scroll to bottom when new messages arrive
    }
    
    if (showNewChatDialog) {
        NewChatDialog(
            onConfirm = { title ->
                chatViewModel.createNewConversation(title)
                showNewChatDialog = false
            },
            onDismiss = { showNewChatDialog = false }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val currentConv = conversations.find { it.id == currentConversationId }
                    Text(currentConv?.title ?: "Unfiltered AI Chat")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSidebar = !showSidebar },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Menu, "Toggle Sidebar")
                    }
                }
            )
        },
        bottomBar = {
            if (currentConversationId != null) {
                ChatInputBar(
                    userInput = userInput,
                    onInputChange = { userInput = it },
                    onSend = {
                        if (userInput.isNotBlank()) {
                            chatViewModel.sendMessage(userInput)
                            userInput = ""
                        }
                    },
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sidebar with conversations
            if (showSidebar) {
                ConversationSidebar(
                    conversations = conversations,
                    selectedConversationId = currentConversationId,
                    onSelectConversation = { chatViewModel.selectConversation(it.id) },
                    onNewChat = { showNewChatDialog = true },
                    onDeleteConversation = { chatViewModel.deleteConversation(it.id) },
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                )
                Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
            }
            
            // Chat area
            if (currentConversationId != null) {
                ChatArea(
                    messages = messages,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onErrorDismiss = { chatViewModel.clearErrorMessage() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            "Start Chat",
                            modifier = Modifier
                                .size(64.dp)
                                .padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Start a new conversation",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(onClick = { showNewChatDialog = true }) {
                            Text("New Chat")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationSidebar(
    conversations: List<Conversation>,
    selectedConversationId: String?,
    onSelectConversation: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (Conversation) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .fillMaxHeight()
    ) {
        Button(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(Icons.Default.Add, "New Chat", modifier = Modifier.padding(end = 8.dp))
            Text("New Chat")
        }
        
        Divider(modifier = Modifier.fillMaxWidth())
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(conversations) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    isSelected = conversation.id == selectedConversationId,
                    onSelect = { onSelectConversation(conversation) },
                    onDelete = { onDeleteConversation(conversation) }
                )
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Chat?") },
            text = { Text("This will delete the entire conversation.") },
            confirmButton = {
                Button(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            conversation.title,
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = true, onClick = onSelect),
            maxLines = 1,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium
        )
        IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                "Delete",
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun ChatArea(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    errorMessage: String?,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(modifier = modifier.fillMaxHeight()) {
        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                action = {
                    TextButton(onClick = onErrorDismiss) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(errorMessage)
            }
        }
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatMessageBubble(message)
            }
            
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage
) {
    val isUser = message.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiaryContainer
                )
                .padding(12.dp)
                .widthIn(max = 250.dp)
        ) {
            Text(
                message.content,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ChatInputBar(
    userInput: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = userInput,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            placeholder = { Text("Ask anything...") },
            singleLine = false,
            shape = RoundedCornerShape(8.dp)
        )
        
        IconButton(
            onClick = onSend,
            enabled = !isLoading && userInput.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                Icons.Default.Send,
                "Send",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun NewChatDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chat") },
        text = {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Chat Title") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.ifBlank { "Untitled Chat" }) },
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
