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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.apollographql.apollo.api.Query;
import com.arcblock.corekit.CoreKitPagedQueryResultListener;
import com.arcblock.corekit.CoreKitPagedQuery;
import com.arcblock.corekit.PagedQueryHelper;
import com.arcblock.corekit.utils.CoreKitDiffUtil;
import com.arcblock.sdk.demo.DemoApplication;
import com.arcblock.sdk.demo.R;
import com.arcblock.sdk.demo.adapter.ListBlocksAdapter;
import com.arcblock.sdk.demo.btc.BlocksByHeightQuery;
import com.arcblock.sdk.demo.btc.type.PageInput;
import com.chad.library.adapter.base.BaseQuickAdapter;

import java.util.ArrayList;
import java.util.List;

public class QueryBlocksByHeightActivity extends AppCompatActivity {

    private ListBlocksAdapter mListBlocksAdapter;

    SwipeRefreshLayout content;
    ProgressBar progressBar;

    private List<BlocksByHeightQuery.Datum> mBlocks = new ArrayList<>();
    private int startIndex = 448244;
    private int endIndex = 448344;
    private CoreKitPagedQuery<BlocksByHeightQuery.Data, BlocksByHeightQuery.Datum> mCoreKitPagedQuery;
    private PagedQueryHelper<BlocksByHeightQuery.Data, BlocksByHeightQuery.Datum> mPagedQueryHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_list_blocks);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.query_list_blocks_data);

        content = (SwipeRefreshLayout) findViewById(R.id.content_holder);
        progressBar = (ProgressBar) findViewById(R.id.loading_bar);

        content.setProgressBackgroundColorSchemeResource(android.R.color.white);
        content.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark);

        content.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mBlocks.clear();
                mListBlocksAdapter.notifyDataSetChanged();

                mListBlocksAdapter.setEnableLoadMore(false);
                mCoreKitPagedQuery.startInitQuery();
            }
        });

        RecyclerView feedRecyclerView = (RecyclerView) findViewById(R.id.rv_feed_list);
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mListBlocksAdapter = new ListBlocksAdapter(R.layout.item_list_blocks, mBlocks);
        mListBlocksAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                mCoreKitPagedQuery.startLoadMoreQuery();
            }
        }, feedRecyclerView);

        // mListBlocksAdapter.openLoadAnimation(BaseQuickAdapter.SLIDEIN_LEFT);
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

        // init page query help
        mPagedQueryHelper = new PagedQueryHelper<BlocksByHeightQuery.Data, BlocksByHeightQuery.Datum>() {
            @Override
            public Query getInitialQuery() {
                return BlocksByHeightQuery.builder().fromHeight(startIndex).toHeight(endIndex).build();
            }

            @Override
            public Query getLoadMoreQuery() {
                PageInput pageInput = null;
                if (!TextUtils.isEmpty(getCursor())) {
                    pageInput = PageInput.builder().cursor(getCursor()).build();
                }
                return BlocksByHeightQuery.builder().fromHeight(startIndex).toHeight(endIndex).paging(pageInput).build();
            }

            @Override
            public List<BlocksByHeightQuery.Datum> map(BlocksByHeightQuery.Data data) {
                if (data.getBlocksByHeight() != null) {
                    // set page info to PagedQueryHelper
                    if (data.getBlocksByHeight().getPage() != null) {
                        // set is have next flag to PagedQueryHelper
                        setHasMore(data.getBlocksByHeight().getPage().isNext());
                        // set new cursor to PagedQueryHelper
                        setCursor(data.getBlocksByHeight().getPage().getCursor());
                    }
                    return data.getBlocksByHeight().getData();
                }
                return null;
            }
        };
        // init a CoreKitPagedQuery and set result listener
        mCoreKitPagedQuery = new CoreKitPagedQuery(this, DemoApplication.getInstance().abCoreKitClientBtc(), mPagedQueryHelper);
        mCoreKitPagedQuery.setPagedQueryResultListener(new CoreKitPagedQueryResultListener<BlocksByHeightQuery.Datum>() {
            @Override
            public void onSuccess(List<BlocksByHeightQuery.Datum> datas) {
                // new a old list
                List<BlocksByHeightQuery.Datum> oldList = new ArrayList<>();
                oldList.addAll(mBlocks);
                // set mBlocks with new data
                mBlocks = datas;
                DiffUtil.DiffResult result = DiffUtil.calculateDiff(new CoreKitDiffUtil<>(oldList, mBlocks), true);
                // need this line , otherwise the update will have no effect
                mListBlocksAdapter.setNewListData(mBlocks);
                result.dispatchUpdatesTo(mListBlocksAdapter);
            }

            @Override
            public void onError(String errMsg) {
                Toast.makeText(QueryBlocksByHeightActivity.this, errMsg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {
                content.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                content.setRefreshing(false);
                if (mPagedQueryHelper.isHasMore()) {
                    mListBlocksAdapter.setEnableLoadMore(true);
                    mListBlocksAdapter.loadMoreComplete();
                } else {
                    mListBlocksAdapter.loadMoreEnd();
                }
            }
        });
        // start initial query
        mCoreKitPagedQuery.startInitQuery();
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
