package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY timestamp DESC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Int): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)
}

@Dao
interface CpfQueryHistoryDao {
    @Query("SELECT * FROM cpf_query_history ORDER BY timestamp DESC")
    fun getAllQueries(): Flow<List<CpfQueryHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(history: CpfQueryHistory): Long

    @Query("DELETE FROM cpf_query_history WHERE id = :id")
    suspend fun deleteQueryById(id: Int)
}

@Dao
interface ProposalDao {
    @Query("SELECT * FROM proposals ORDER BY timestamp DESC")
    fun getAllProposals(): Flow<List<Proposal>>

    @Query("SELECT * FROM proposals WHERE clientId = :clientId ORDER BY timestamp DESC")
    fun getProposalsByClient(clientId: Int): Flow<List<Proposal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProposal(proposal: Proposal): Long

    @Update
    suspend fun updateProposal(proposal: Proposal)

    @Delete
    suspend fun deleteProposal(proposal: Proposal)
}
