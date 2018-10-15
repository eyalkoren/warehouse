package eyal.koren.tracing.api;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection", "UnusedReturnValue", "WeakerAccess"})
public abstract class AbstractSpan {
    // An example for an API common for Transactions and Spans
    public abstract void commonTransactionSpanApi();

    public class Transaction extends AbstractSpan {
        private String user;
        private String name;

        private List<AsyncSpanFuture> asyncSpanFutures = new ArrayList<>();

        @Override
        public void commonTransactionSpanApi() {
            // implement transaction behaviour
        }

        public String getUser() {
            for(AsyncSpanFuture asyncResult: asyncSpanFutures)
            {
                if(asyncResult.user != null)
                    return asyncResult.user;
            }
            return user;
        }

        public Transaction withUser(String user) {
            this.user = user;
            return this;
        }

        public String getName() {
            for(AsyncSpanFuture asyncResult: asyncSpanFutures)
            {
                if(asyncResult.name != null)
                    return asyncResult.name;
            }
            return name;
        }

        public Transaction withName(String name) {
            this.name = name;
            return this;
        }

        public Span createSyncSpan() {
            return new SyncSpan(this);
        }

        public Span createAsyncSpan() {
            AsyncSpanFuture result = new AsyncSpanFuture();
            asyncSpanFutures.add(result);
            return new AsyncSpan(result);
        }

        class AsyncSpanFuture {
            String user;
            String name;
        }
    }

    public abstract class Span extends AbstractSpan {
        @Override
        public void commonTransactionSpanApi() {
            // implement span behaviour
        }

        // Just an example for something that both relevant to sync and async but needs to be implemented differently
        public abstract void setUser(String user);

        public abstract void setName(String name);
    }

    private class SyncSpan extends Span {
        private Transaction transaction;

        public SyncSpan(Transaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public void setUser(String user) {
            this.transaction.withUser(user);
        }

        @Override
        public void setName(String name) {
            this.transaction.withName(name);
        }
    }

    private class AsyncSpan extends Span {
        private Transaction.AsyncSpanFuture result;

        public AsyncSpan(Transaction.AsyncSpanFuture result) {
            this.result = result;
        }

        @Override
        public void setUser(String user) {
            this.result.user = user;
        }

        @Override
        public void setName(String name) {
            this.result.name = name;
        }
    }
}
