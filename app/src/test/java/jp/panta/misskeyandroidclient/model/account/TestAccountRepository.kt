package jp.panta.misskeyandroidclient.model.account

class TestAccountRepository : AccountRepository {

    val accounts = mutableMapOf(
        1L to Account("remote1", "test.misskey.jp", "test1", "token", emptyList(), Account.InstanceType.MISSKEY,1),
        2L to Account("remote2", "test.misskey.jp", "test2", "token", emptyList(), Account.InstanceType.MISSKEY,2),
        3L to Account("remote3", "test.misskey.jp", "test1", "token", emptyList(), Account.InstanceType.MISSKEY,3)
    )

    private var currentAccountId = 1L

    override suspend fun add(account: Account, isUpdatePages: Boolean): Account {
        val ac = account.copy(
            accountId = accounts.size.toLong()
        )
        accounts[accounts.size.toLong()] = ac
        return ac
    }

    override fun addEventListener(listener: AccountRepository.Listener) {

    }

    override fun removeEventListener(listener: AccountRepository.Listener) {

    }

    override suspend fun delete(account: Account) {
        accounts.remove(account.accountId)
    }

    override suspend fun findAll(): List<Account> {
        return accounts.values.toList()
    }

    override suspend fun get(accountId: Long): Account {
        return accounts[accountId]?: throw AccountNotFoundException()
    }

    override suspend fun getCurrentAccount(): Account {
        return get(currentAccountId)
    }

    override suspend fun setCurrentAccount(account: Account): Account {
        currentAccountId = account.accountId
        return account
    }
}