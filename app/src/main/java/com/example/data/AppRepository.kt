package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val database: AppDatabase) {
    private val clientDao = database.clientDao()
    private val cpfDao = database.cpfQueryHistoryDao()
    private val proposalDao = database.proposalDao()

    // Client operations
    val allClients: Flow<List<Client>> = clientDao.getAllClients()

    suspend fun getClientById(id: Int): Client? {
        return clientDao.getClientById(id)
    }

    suspend fun insertClient(client: Client): Long {
        return clientDao.insertClient(client)
    }

    suspend fun updateClient(client: Client) {
        clientDao.updateClient(client)
    }

    suspend fun deleteClient(client: Client) {
        clientDao.deleteClient(client)
    }

    // CPF Query History operations
    val allQueries: Flow<List<CpfQueryHistory>> = cpfDao.getAllQueries()

    suspend fun insertCpfQuery(query: CpfQueryHistory): Long {
        return cpfDao.insertQuery(query)
    }

    suspend fun deleteQueryById(id: Int) {
        cpfDao.deleteQueryById(id)
    }

    // Proposal operations
    val allProposals: Flow<List<Proposal>> = proposalDao.getAllProposals()

    fun getProposalsByClient(clientId: Int): Flow<List<Proposal>> {
        return proposalDao.getProposalsByClient(clientId)
    }

    suspend fun insertProposal(proposal: Proposal): Long {
        return proposalDao.insertProposal(proposal)
    }

    suspend fun updateProposal(proposal: Proposal) {
        proposalDao.updateProposal(proposal)
    }

    suspend fun deleteProposal(proposal: Proposal) {
        proposalDao.deleteProposal(proposal)
    }
}
