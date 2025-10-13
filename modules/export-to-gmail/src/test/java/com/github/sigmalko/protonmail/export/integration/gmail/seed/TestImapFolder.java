package com.github.sigmalko.protonmail.export.integration.gmail.seed;

import java.util.Date;
import java.util.Properties;

import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.URLName;
import jakarta.mail.search.SearchTerm;

class TestImapFolder extends Folder {

        Message[] appendedMessages;
        Message appendedMessage;
        Flags appendedFlags;
        Date appendedDate;
        boolean throwOnImapAppend;

        TestImapFolder() {
                super(new DummyStore());
        }

        @Override
        public String getName() {
                return "test";
        }

        @Override
        public String getFullName() {
                return "test";
        }

        @Override
        public Folder getParent() {
                return null;
        }

        @Override
        public boolean exists() {
                return true;
        }

        @Override
        public Folder[] list(String pattern) {
                return new Folder[0];
        }

        @Override
        public char getSeparator() {
                return '/';
        }

        @Override
        public int getType() {
                return HOLDS_MESSAGES;
        }

        @Override
        public boolean create(int type) {
                return false;
        }

        @Override
        public boolean hasNewMessages() {
                return false;
        }

        @Override
        public Folder getFolder(String name) {
                return null;
        }

        @Override
        public boolean delete(boolean recurse) {
                return false;
        }

        @Override
        public boolean renameTo(Folder f) {
                return false;
        }

        @Override
        public void open(int mode) {
                // no-op
        }

        @Override
        public void close(boolean expunge) {
                // no-op
        }

        @Override
        public boolean isOpen() {
                return true;
        }

        @Override
        public Flags getPermanentFlags() {
                return new Flags();
        }

        @Override
        public int getMessageCount() {
                return appendedMessages == null ? 0 : appendedMessages.length;
        }

        @Override
        public Message getMessage(int msgnum) {
                throw new UnsupportedOperationException();
        }

        @Override
        public void appendMessages(Message[] messages) {
                appendedMessages = messages;
        }

        @Override
        public Message[] expunge() {
                return new Message[0];
        }

        @Override
        public Message[] search(SearchTerm term) {
                return new Message[0];
        }

        @Override
        public Message[] getMessages() {
                return appendedMessages == null ? new Message[0] : appendedMessages.clone();
        }

        @Override
        public int getUnreadMessageCount() {
                return 0;
        }

        @Override
        public int getNewMessageCount() {
                return 0;
        }

        @Override
        public int getDeletedMessageCount() {
                return 0;
        }

        @Override
        public void fetch(Message[] msgs, FetchProfile fp) {
                // no-op
        }

        public void appendMessage(Message message, Flags flags, Date internalDate) throws MessagingException {
                if (throwOnImapAppend) {
                        throw new MessagingException("failure");
                }
                appendedMessage = message;
                appendedFlags = flags;
                appendedDate = internalDate;
        }

        private static final class DummyStore extends Store {

                DummyStore() {
                        super(Session.getInstance(new Properties()), (URLName) null);
                }

                @Override
                public Folder getDefaultFolder() {
                        throw new UnsupportedOperationException();
                }

                @Override
                public Folder getFolder(String name) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public Folder getFolder(URLName url) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public void connect(String host, int port, String user, String password) {
                        // no-op
                }

                @Override
                public void connect(String host, String user, String password) {
                        // no-op
                }

                @Override
                public void connect(String user, String password) {
                        // no-op
                }

                @Override
                public void connect() {
                        // no-op
                }

                @Override
                public boolean isConnected() {
                        return true;
                }

                @Override
                public void close() {
                        // no-op
                }

                @Override
                public URLName getURLName() {
                        return null;
                }
        }
}
