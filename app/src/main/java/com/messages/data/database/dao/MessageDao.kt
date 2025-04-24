package com.messages.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.messages.data.models.Contact
import com.messages.data.models.Conversation
import com.messages.data.models.Message
import com.messages.data.models.Recipient

@Dao
interface MessageDao {

    /* ======  Insert  ====== */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConversations(conversationModel: Conversation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContact(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContacts(contact: List<Contact>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMessages(messages: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecipient(recipient: Recipient): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecipients(recipient: List<Recipient>)

    /* ======  get conversations  ====== */

    @Query("SELECT * FROM conversations ORDER BY date DESC")
    fun getAllConversation(): List<Conversation>

    @Query("SELECT * FROM conversations WHERE isArchived = 0 AND isPrivate = 0 AND isBlocked = 0 ORDER BY date DESC")
    fun getConversations(): List<Conversation>

    /*    @Query("""
       SELECT c.*,
              COALESCE((SELECT COUNT(*) FROM messages WHERE threadId = c.id AND read = 0), 0) AS unreadCount,
              COALESCE((SELECT subId FROM messages WHERE threadId = c.id ORDER BY date DESC LIMIT 1), -1) AS subId,
              COALESCE((SELECT simSlot FROM messages WHERE threadId = c.id ORDER BY date DESC LIMIT 1), -1) AS simSlot
       FROM conversations c
       WHERE c.archived = 0
         AND c.isPrivate = 0
         AND c.isBlocked = 0
       ORDER BY c.date DESC""")
       fun getConversationsWithUnreadAndSubId(): List<Conversation>*/

    @Query(
        """
    SELECT c.*,COALESCE(m.unreadCount, 0) AS unreadCount,COALESCE(latest.subId, -1) AS subId,COALESCE(latest.simSlot, -1) AS simSlot
     FROM conversations c LEFT JOIN (SELECT threadId, COUNT(*) AS unreadCount FROM messages WHERE read = 0 GROUP BY threadId
      ) m ON c.threadId = m.threadId LEFT JOIN (
    SELECT m1.threadId, m1.subId, m1.simSlot FROM messages m1 INNER JOIN (SELECT threadId, MAX(date) AS maxDate 
        FROM messages GROUP BY threadId ) m2 ON m1.threadId = m2.threadId AND m1.date = m2.maxDate
     ) latest ON c.threadId = latest.threadId
    WHERE c.isPrivate = 0 AND c.isBlocked = 0 AND c.isSchedule = 0 AND c.isArchived = 0 ORDER BY c.date DESC
     """
    )
    fun getConversationsWithUnreadAndSubId(): List<Conversation>

    @Query(
        """
    SELECT c.*,COALESCE(m.unreadCount, 0) AS unreadCount,COALESCE(latest.subId, -1) AS subId,COALESCE(latest.simSlot, -1) AS simSlot
     FROM conversations c LEFT JOIN (SELECT threadId, COUNT(*) AS unreadCount FROM messages WHERE read = 0 GROUP BY threadId
      ) m ON c.threadId = m.threadId LEFT JOIN (
    SELECT m1.threadId, m1.subId, m1.simSlot FROM messages m1 INNER JOIN (SELECT threadId, MAX(date) AS maxDate 
        FROM messages GROUP BY threadId ) m2 ON m1.threadId = m2.threadId AND m1.date = m2.maxDate
     ) latest ON c.threadId = latest.threadId
    WHERE c.threadId = :threadId AND c.isPrivate = 0 AND c.isBlocked = 0 AND c.isSchedule = 0 AND c.isArchived = 0
     """
    )
    fun getConversationsUsingThreadId(threadId: Long): List<Conversation>

    @Query("SELECT * FROM conversations WHERE threadId IN (SELECT DISTINCT threadId FROM messages) ORDER BY date DESC")
    fun getAllConversations(): List<Conversation>

    @Query("SELECT MAX(date) FROM conversations")
    suspend fun getLatestConversationDate(): Long?

    @Query("SELECT threadId FROM conversations")
    suspend fun getThreadIdsOfCurrentConversations(): List<Long>

    @Query("SELECT * FROM conversations WHERE threadId = :threadId")
    fun getConversationOfThreadId(threadId: Long): Conversation

    @Query("SELECT * FROM conversations WHERE isArchived = 1")
    fun getArchivedConversations(): List<Conversation>

    @Query(
        """
    SELECT c.*, 
           COALESCE((SELECT COUNT(*) FROM messages WHERE threadId = c.threadId AND read = 0), 0) AS unreadCount,
           COALESCE((SELECT subId FROM messages WHERE threadId = c.threadId ORDER BY date DESC LIMIT 1), -1) AS subId
    FROM conversations c WHERE c.isArchived = 1 ORDER BY c.date DESC"""
    )
    fun getArchivedConversation(): List<Conversation>

    @Query("SELECT * FROM conversations WHERE isPrivate = 1")
    fun getPrivateConversations(): List<Conversation>

    @Query(
        """
    SELECT c.*, 
           COALESCE((SELECT COUNT(*) FROM messages WHERE threadId = c.threadId AND read = 0), 0) AS unreadCount,
           COALESCE((SELECT subId FROM messages WHERE threadId = c.threadId ORDER BY date DESC LIMIT 1), -1) AS subId
    FROM conversations c WHERE c.isPrivate = 1 ORDER BY c.date DESC"""
    )
    fun getPrivateConversation(): List<Conversation>

    @Query("SELECT * FROM conversations WHERE threadId = :threadId")
    fun getConversationFromThreadId(threadId: Long): Conversation?

    @Query("SELECT * FROM conversations WHERE name LIKE '%' || :searchText || '%' OR snippet LIKE '%' || :searchText || '%'")
    fun findConversationBySearch(searchText: String): List<Conversation>

    @Query("SELECT isPrivate FROM conversations WHERE threadId = :threadId")
    fun checkIsPrivateConversation(threadId: Long): Boolean

    @Query("SELECT recipients FROM conversations WHERE threadId = :threadId")
    fun getThreadRecipient(threadId: Long): String?

    @Query(
        """
    SELECT c.*, m.body as snippet, m.date as date FROM conversations c
    INNER JOIN messages m ON c.threadId = m.threadId
    WHERE m.isScheduled = 1
    AND m.date = (SELECT MAX(date) FROM messages WHERE threadId = c.threadId AND isScheduled = 1) ORDER BY c.date DESC"""
    )
    fun getScheduleConversations(): List<Conversation>

    @Query("SELECT * FROM messages WHERE isScheduled = 1 ORDER BY date DESC")
    fun getScheduleMessages(): List<Message>

    @Query("SELECT * FROM conversations WHERE isBlocked = 1")
    fun getBlockedConversations(): List<Conversation>

    @Query("""SELECT * FROM conversations WHERE :recipientId IN (SELECT recipientId FROM Recipient WHERE recipientId = :recipientId)""")
    fun getConversationAccordingToRecipient(recipientId: Long): List<Conversation>

//    @Query("SELECT * FROM conversations WHERE recipients")
//    fun getConversationsByNumber(number: String): List<Conversation>

    /* ======  get contacts  ====== */

    @Query("SELECT * FROM contacts ORDER BY lastUpdate DESC")
    fun getAllContacts(): List<Contact>

    @Query("SELECT * FROM contacts WHERE isFavorite = 1")
    fun getFavoriteContacts(): List<Contact>

    @Query("SELECT * FROM contacts WHERE isFavorite = 0")
    fun getContacts(): List<Contact>

    @Query("SELECT MAX(lastUpdate) FROM contacts")
    suspend fun getLatestContactModifyDate(): Long?

    @Query("SELECT * FROM contacts WHERE contactId = :contactId")
    suspend fun getContact(contactId: Long): List<Contact>

    @Query("SELECT * FROM contacts WHERE phoneNumbers LIKE '%' || :number || '%'")
    fun getContactsFromNumber(number: String): List<Contact>

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :searchText || '%' OR phoneNumbers LIKE '%' || :searchText || '%' ORDER BY name ASC")
    fun findContactBySearch(searchText: String): List<Contact>

    @Query("SELECT * FROM contacts WHERE lastUpdate = :lastUpdate")
    suspend fun getContactsByTime(lastUpdate: Long): List<Contact>

    @Query("SELECT * FROM contacts WHERE phoneNumbers LIKE '%' || :number || '%'")
    fun getContactFromNumber(number: String): Contact?

    /* ======  get messages  ====== */

    @Query("SELECT * FROM messages ORDER BY date DESC")
    fun getAllMessages(): List<Message>

    @Query("SELECT MAX(date) FROM messages WHERE threadId = :threadId")
    suspend fun getLatestMessageDate(threadId: Long): Long?

    @Query("SELECT MAX(date) FROM messages")
    suspend fun getLatestMessageDate(): Long?

    @Query("SELECT MAX(id) FROM messages")
    fun getLastMessageId(): Long

    @Query("SELECT id FROM messages WHERE messageId = :messageId")
    fun getIdOfMessage(messageId: Long): Long?

    @Query("SELECT COUNT(*) FROM messages WHERE threadId = :threadId AND read = 0")
    fun gerUnreadMessageCount(threadId: Long): Int

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY date DESC")
    fun getMessagesFromThreadId(threadId: Long): List<Message>

    @Query("SELECT messageId FROM messages WHERE threadId = :threadId")
    fun getMessageIdsFromThreadId(threadId: Long): List<Long>

    @Query("SELECT * FROM messages WHERE threadId = :threadId AND messageId = :messageId AND isScheduled = 1")
    fun getScheduledMessageWithId(threadId: Long, messageId: Long): Message

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY date DESC LIMIT 1")
    fun getLastMessageFromThreadId(threadId: Long): Message?

    @Query("""SELECT subId FROM messages WHERE threadId = :threadId ORDER BY date DESC LIMIT 1""")
    fun gerMessageSubId(threadId: Long): Int

    /* ======  get Recipient  ====== */

    @Query("SELECT * FROM recipient WHERE recipientId IN (:recipientIds)")
    fun getRecipientsByIds(recipientIds: List<Long>): List<Recipient>

    @Query("SELECT * FROM recipient WHERE recipientId = :recipientId")
    fun getRecipientsById(recipientId: Long): Recipient?

    @Query("SELECT * FROM recipient WHERE address = :address ORDER BY id DESC")
    fun getRecipientsByAddress(address: String): List<Recipient>

    @Query("SELECT MAX(lastUpdate) FROM recipient")
    suspend fun getLatestRecipientDate(): Long

    @Query("SELECT * FROM recipient WHERE lastUpdate > :date")
    fun getRecipientsByDate(date: Long): List<Recipient>

    @Query("SELECT * FROM recipient")
    fun getAllRecipients(): List<Recipient>

    @Query("SELECT COUNT() FROM recipient")
    fun getAllRecipientsCount(): Int

    /* ======  update  ====== */

    @Query("UPDATE conversations SET snippet= :snippet, date = :date  WHERE threadId = :threadId")
    suspend fun updateConversationData(threadId: Long, snippet: String, date: Long)

    @Query("UPDATE conversations SET isArchived = :isArchive WHERE threadId IN (:threadIds)")
    suspend fun handleConversationForArchived(threadIds: List<Long>, isArchive: Boolean)

    @Query("UPDATE conversations SET read = :readUnread WHERE threadId IN (:threadIds)")
    suspend fun handleConversationForRead(threadIds: List<Long>, readUnread: Boolean)

    @Query("UPDATE conversations SET isPrivate = :isPrivate WHERE threadId IN (:threadIds)")
    suspend fun handleConversationForPrivate(threadIds: List<Long>, isPrivate: Boolean)

    @Query("UPDATE messages SET type = :type , deliveryStatus =:status WHERE messageId = :id")
    fun updateMessagesType(id: Long, type: Int, status: Int): Int

    @Query("UPDATE conversations SET isPined= :isPined  WHERE threadId IN (:threadIds)")
    suspend fun updateConversationPinedData(threadIds: List<Long>, isPined: Boolean)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE messageId = :id")
    fun updateMessagesStatus(id: Long, status: Int): Int

    @Query("UPDATE conversations SET isBlocked = :isBlocked WHERE threadId IN (:threadIds)")
    suspend fun handleConversationForBlocked(threadIds: List<Long>, isBlocked: Boolean)

    @Query("UPDATE conversations SET recipients = :recipient, name = :name  WHERE threadId = :threadId")
    fun updateThreadRecipient(threadId: Long, recipient: List<Recipient>, name: String)

    @Query("UPDATE messages SET read = :readUnread WHERE threadId IN (:threadIds)")
    suspend fun updateMessagesAsRead(threadIds: List<Long>, readUnread: Boolean)

    @Query("UPDATE messages SET read = 1 WHERE threadId = :threadId")
    suspend fun updateMessagesAsRead(threadId: Long)

    @Query("UPDATE conversations SET isSchedule = :isSchedule WHERE threadId = :threadId")
    suspend fun updateScheduleStatusOfConversation(threadId: Long, isSchedule: Boolean)

    /* ======  delete  ====== */

    @Query("DELETE FROM messages WHERE messageId = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM conversations WHERE threadId IN (:threadIds)")
    suspend fun deleteConversations(threadIds: List<Long>)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteScheduledMessage(messageId: Long)

    @Query("DELETE FROM conversations WHERE threadId = :threadIds")
    suspend fun deleteTemporaryThreadId(threadIds: Long)

    @Query("DELETE FROM messages WHERE threadId IN (:threadIds)")
    suspend fun deleteScheduleMessages(threadIds: List<Long>)

    @Query("DELETE FROM messages WHERE threadId IN (:threadIds)")
    suspend fun deleteThreadMessages(threadIds: List<Long>)
}