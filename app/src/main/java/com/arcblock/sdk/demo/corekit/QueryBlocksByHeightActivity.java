/*
 * Copyright (c) 2017-present ArcBlock Foundation Ltd <https://www.arcblock.io/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.arcblock.sdk.demo.corekit;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.apollographql.apollo.api.Response;
import com.arcblock.corekit.bean.CoreKitBean;
import com.arcblock.corekit.bean.CoreKitBeanMapper;
import com.arcblock.corekit.bean.CoreKitPagedBean;
import com.arcblock.corekit.utils.CoreKitDiffUtil;
import com.arcblock.corekit.viewmodel.CoreKitPagedViewModel;
import com.arcblock.sdk.demo.BlocksByHeightQuery;
import com.arcblock.sdk.demo.DemoApplication;
import com.arcblock.sdk.demo.R;
import com.arcblock.sdk.demo.adapter.ListBlocksAdapter;
import com.chad.library.adapter.base.BaseQuickAdapter;

import java.util.ArrayList;
import java.util.List;

public class QueryBlocksByHeightActivity extends AppCompatActivity {

	private ListBlocksAdapter mListBlocksAdapter;

	SwipeRefreshLayout content;
	ProgressBar progressBar;

	private List<BlocksByHeightQuery.Datum> mBlocks = new ArrayList<>();

	private CoreKitPagedViewModel<BlocksByHeightQuery.Data, BlocksByHeightQuery.BlocksByHeight, BlocksByHeightQuery.Datum> mBlocksByHeightQueryViewModel;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_query_list_blocks);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(R.string.query_list_blocks_data);

		content = (SwipeRefreshLayout) findViewById(R.id.content_holder);
		progressBar = (ProgressBar) findViewById(R.id.loading_bar);

		content.setEnabled(false);
		content.setProgressBackgroundColorSchemeResource(android.R.color.white);
		content.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark);

		content.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				mBlocksByHeightQueryViewModel.refresh();
				mListBlocksAdapter.setEnableLoadMore(false);
			}
		});

		RecyclerView feedRecyclerView = (RecyclerView) findViewById(R.id.rv_feed_list);
		feedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		mListBlocksAdapter = new ListBlocksAdapter(R.layout.item_list_blocks, mBlocks);
		mListBlocksAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
			@Override
			public void onLoadMoreRequested() {
				mBlocksByHeightQueryViewModel.loadMore();
			}
		}, feedRecyclerView);

		mListBlocksAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
				Intent intent = new Intent(QueryBlocksByHeightActivity.this, BlockDetailActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString(BlockDetailActivity.BLOCK_HASH_KEY, mBlocks.get(position).getHash());
				intent.putExtras(bundle);
				startActivity(intent);
			}
		});
		feedRecyclerView.setAdapter(mListBlocksAdapter);

		// init data mapper
		CoreKitBeanMapper<Response<BlocksByHeightQuery.Data>, BlocksByHeightQuery.BlocksByHeight> blocksMapper = new CoreKitBeanMapper<Response<BlocksByHeightQuery.Data>, BlocksByHeightQuery.BlocksByHeight>() {

			@Override
			public BlocksByHeightQuery.BlocksByHeight map(Response<BlocksByHeightQuery.Data> dataResponse) {
				if (dataResponse != null) {
					return dataResponse.data().getBlocksByHeight();
				}
				return null;
			}
		};
		// init a query
		BlocksByHeightQuery query = BlocksByHeightQuery.builder().fromHeight(448244).toHeight(448254).build();
		// init the ViewModel with CustomClientFactory
		CoreKitPagedViewModel.CustomClientFactory factory = new CoreKitPagedViewModel.CustomClientFactory(blocksMapper, DemoApplication.getInstance().abCoreKitClient());
		mBlocksByHeightQueryViewModel = ViewModelProviders.of(this, factory).get(CoreKitPagedViewModel.class);
		mBlocksByHeightQueryViewModel.getQueryData(query).observe(this, new Observer<CoreKitPagedBean<List<BlocksByHeightQuery.Datum>>>() {
			@Override
			public void onChanged(@Nullable CoreKitPagedBean<List<BlocksByHeightQuery.Datum>> coreKitPagedBean) {
				content.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);

				mListBlocksAdapter.setEnableLoadMore(true);
				content.setRefreshing(false);
				mListBlocksAdapter.loadMoreComplete();
				if (!mBlocksByHeightQueryViewModel.isNext()) {
					mListBlocksAdapter.loadMoreEnd();
				}

				if (coreKitPagedBean.getStatus() == CoreKitBean.SUCCESS_CODE) {
					if (coreKitPagedBean.getData() != null) {
						List<BlocksByHeightQuery.Datum> oldList = mBlocks;
						List<BlocksByHeightQuery.Datum> newList = coreKitPagedBean.getData();
						DiffUtil.DiffResult result = DiffUtil.calculateDiff(new CoreKitDiffUtil<>(oldList, newList), true);
						mListBlocksAdapter.setNewListData(newList);
						result.dispatchUpdatesTo(mListBlocksAdapter);
					}
				} else {
					// todo show error msg.
				}
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home://返回键的id
				this.finish();
				return false;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
