package com.example.women_safety.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.women_safety.R
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class ChatbotFragment : Fragment() {

    private lateinit var messageEditText: TextInputEditText
    private lateinit var sendButton: Button
    private lateinit var messagesRecyclerView: RecyclerView
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    // Simple responses for the chatbot
    private val botResponses = mapOf(
        "hello" to "Hello! How can I help you with safety today?",
        "hi" to "Hi there! I'm your safety assistant. How can I help?",
        "help" to "If you're in immediate danger, press the SOS button on the home screen. I can also provide safety tips and resources.",
        "safety" to "Always stay aware of your surroundings, keep your phone charged, and let someone know where you are going.",
        "scared" to "It's okay to feel scared. Trust your instincts. If you feel unsafe, move to a public place and use the SOS feature if needed.",
        "tip" to "Safety Tip: Program emergency numbers for quick access. Consider using code words with friends and family for emergencies.",
        "emergency" to "If you're in an emergency situation, please use the SOS button on the home screen or call local emergency services immediately.",
        "self defense" to "Basic self-defense includes being aware of surroundings, projecting confidence, and knowing pressure points. Consider taking a self-defense class.",
        "bye" to "Stay safe! Remember I'm here if you need assistance."
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chatbot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        messageEditText = view.findViewById(R.id.et_message)
        sendButton = view.findViewById(R.id.btn_send)
        messagesRecyclerView = view.findViewById(R.id.rv_chat_messages)

        // Set up RecyclerView
        chatAdapter = ChatAdapter(chatMessages)
        messagesRecyclerView.layoutManager = LinearLayoutManager(context)
        messagesRecyclerView.adapter = chatAdapter

        // Add welcome message
        addBotMessage("Hi! I'm your safety assistant. How can I help you today?")

        // Easter egg: If you type "secret code" the bot gives a special response
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                addUserMessage(message)
                messageEditText.setText("")

                // Check for Easter egg
                if (message.lowercase() == "secret code") {
                    addBotMessage("🎉 You found the Easter egg! Here's a special safety tip: Create a custom emergency widget on your phone's home screen for quick access. Stay safe! 🎉")
                } else {
                    // Process the message and get response
                    val response = getBotResponse(message)
                    // Add a little delay to make it feel more natural
                    messagesRecyclerView.postDelayed({
                        addBotMessage(response)
                    }, 800)
                }
            }
        }
    }

    private fun addUserMessage(message: String) {
        chatMessages.add(ChatMessage(message, true))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        messagesRecyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun addBotMessage(message: String) {
        chatMessages.add(ChatMessage(message, false))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        messagesRecyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun getBotResponse(message: String): String {
        val lowercaseMessage = message.lowercase(Locale.getDefault())

        // Check if the message contains any of our keywords
        for ((keyword, response) in botResponses) {
            if (lowercaseMessage.contains(keyword)) {
                return response
            }
        }

        // Default responses if no keyword matches
        val defaultResponses = arrayOf(
            "I'm here to help you. Can you tell me more about your concern?",
            "That's an important question. If you're feeling unsafe, remember to use the SOS button.",
            "I understand your concern. Safety is our priority. Is there something specific you need help with?",
            "I'm still learning. Could you ask about safety tips, emergency help, or self-defense?"
        )

        return defaultResponses[Random().nextInt(defaultResponses.size)]
    }
}

// ChatMessage data class and adapter
data class ChatMessage(val message: String, val isUser: Boolean)

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    // View types
    private val VIEW_TYPE_USER = 1
    private val VIEW_TYPE_BOT = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.tv_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_USER) {
            R.layout.item_user_message
        } else {
            R.layout.item_bot_message
        }

        val view = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.messageTextView.text = messages[position].message
    }

    override fun getItemCount() = messages.size
}