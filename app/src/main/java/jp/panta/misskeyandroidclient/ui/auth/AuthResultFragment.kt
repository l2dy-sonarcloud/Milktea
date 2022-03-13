package jp.panta.misskeyandroidclient.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.databinding.FragmentAuthResultBinding
import jp.panta.misskeyandroidclient.model.auth.Authorization
import jp.panta.misskeyandroidclient.model.auth.custom.AccessToken
import jp.panta.misskeyandroidclient.ui.auth.viewmodel.AuthViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
class AuthResultFragment : Fragment(){

    lateinit var binding: FragmentAuthResultBinding

    val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_auth_result, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            viewModel.authorization.collect {
                if(it is Authorization.Approved) {
                    if (it.accessToken is AccessToken.Misskey) {
                        binding.user = it.accessToken.user
                    }
                    binding.continueAuth.isEnabled = true
                }
            }
        }

        binding.continueAuth.setOnClickListener {
            viewModel.confirmApprove()
        }

    }


}