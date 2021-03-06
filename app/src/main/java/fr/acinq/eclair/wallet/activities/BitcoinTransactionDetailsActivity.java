/*
 * Copyright 2018 ACINQ SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.acinq.eclair.wallet.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.text.DateFormat;

import fr.acinq.bitcoin.MilliSatoshi;
import fr.acinq.eclair.CoinUnit;
import fr.acinq.eclair.CoinUtils;
import fr.acinq.eclair.wallet.R;
import fr.acinq.eclair.wallet.adapters.PaymentItemHolder;
import fr.acinq.eclair.wallet.customviews.DataRow;
import fr.acinq.eclair.wallet.models.Payment;
import fr.acinq.eclair.wallet.utils.WalletUtils;

public class BitcoinTransactionDetailsActivity extends EclairActivity {

  private static final String TAG = "BtcTransactionDetails";

  private DataRow mAmountPaidRow;
  private DataRow mFeesRow;
  private DataRow mPaymentHashRow;
  private DataRow mUpdateDateRow;
  private DataRow mTxConfs;
  private DataRow mTxConfsType;
  private View mOpenInExplorer;
  private View mRebroadcastTxView;
  private AlertDialog mRebroadcastDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_bitcoin_transaction_details);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    ActionBar ab = getSupportActionBar();
    ab.setDisplayHomeAsUpEnabled(true);

    mAmountPaidRow = findViewById(R.id.transactiondetails_amount);
    mFeesRow = findViewById(R.id.transactiondetails_fees);
    mPaymentHashRow = findViewById(R.id.transactiondetails_txid);
    mUpdateDateRow = findViewById(R.id.transactiondetails_date);
    mTxConfs = findViewById(R.id.transactiondetails_confs);
    mTxConfsType = findViewById(R.id.transactiondetails_confs_type);
    mOpenInExplorer = findViewById(R.id.open_in_explorer);
    mRebroadcastTxView = findViewById(R.id.transactiondetails_rebroadcast);
    mRebroadcastTxView.setVisibility(View.GONE);
  }

  @Override
  protected void onStart() {
    super.onStart();
    Intent intent = getIntent();
    long paymentId = intent.getLongExtra(PaymentItemHolder.EXTRA_PAYMENT_ID, -1);

    try {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
      final CoinUnit prefUnit = WalletUtils.getPreferredCoinUnit(prefs);

      final Payment p = app.getDBHelper().getPayment(paymentId);

      final AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(getResources().getString(R.string.transactiondetails_rebroadcast_dialog));
      builder.setPositiveButton(R.string.btn_ok, (dialog, id) -> {
        try {
          app.broadcastTx(p.getTxPayload());
          Toast.makeText(getApplicationContext(), "Sent Broadcast", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
          Log.w(TAG, "Could not broadcast tx:" + p.getReference() + "  with cause=" + e.getMessage());
          Toast.makeText(getApplicationContext(), "Broadcast has failed", Toast.LENGTH_LONG).show();
        }
        mRebroadcastDialog.dismiss();
      });
      builder.setNegativeButton(R.string.btn_cancel, (dialog, id) -> mRebroadcastDialog.dismiss());
      mRebroadcastDialog = builder.create();

      mAmountPaidRow.setValue(CoinUtils.formatAmountInUnit(new MilliSatoshi(p.getAmountPaidMsat()), prefUnit, true));
      mFeesRow.setValue(CoinUtils.formatAmountInUnit(new MilliSatoshi(p.getFeesPaidMsat()), prefUnit, true));
      mPaymentHashRow.setValue(p.getReference());
      mUpdateDateRow.setValue(DateFormat.getDateTimeInstance().format(p.getUpdated()));
      mOpenInExplorer.setOnClickListener(WalletUtils.getOpenTxListener(p.getReference()));
      mTxConfs.setValue(Integer.toString(p.getConfidenceBlocks()));
      // TODO confidence type should be human readable
      mTxConfsType.setValue(Integer.toString(p.getConfidenceType()));

      if (p.getConfidenceBlocks() == 0) {
        mRebroadcastTxView.setVisibility(View.VISIBLE);
        mRebroadcastTxView.setOnClickListener(v -> mRebroadcastDialog.show());
      }
    } catch (Exception e) {
      Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show();
      finish();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    checkInit();
  }
}
