package jp.panta.misskeyandroidclient.model.core

import androidx.room.*

@Dao
abstract class AccountDao{

    //@Query("select * from account left join connection_information on account.id = connection_information.accountId left join setting on account.id = setting.accountId")
    @Transaction
    @Query("select * from account")
    abstract fun findAllSetting(): List<AccountRelation>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(account: Account): Long

    @Delete
    abstract fun delete(account: Account)

    //@Query("select * from account left join connection_information on connection_information.accountId = account.id left join setting on account.id = setting.accountId where connection_information.accountId = :accountId")
    @Transaction
    @Query("select * from account where account.id = :accountId")
    abstract fun findSettingByAccountId(accountId: String): AccountRelation?

    @Query("select * from account where account.id = :accountId")
    abstract fun findAccount(accountId: String): Account?

    @Query("drop table account")
    abstract fun dropTable()

    @Query("drop table page")
    abstract fun dropPageTable()

}