/*
 * Copyright (C) 2015 Actor LLC. <https://actor.im>
 */

package im.actor.core.modules.sequence.processor;

import java.util.List;

import im.actor.core.api.ApiGroup;
import im.actor.core.api.ApiUser;
import im.actor.core.api.updates.UpdateMessage;
import im.actor.core.entity.Group;
import im.actor.core.entity.Peer;
import im.actor.core.entity.User;
import im.actor.core.modules.AbsModule;
import im.actor.core.modules.ModuleContext;
import im.actor.core.modules.blocklist.BlockListProcessor;
import im.actor.core.modules.calls.CallsProcessor;
import im.actor.core.modules.contacts.ContactsProcessor;
import im.actor.core.modules.encryption.EncryptedProcessor;
import im.actor.core.modules.eventbus.EventBusProcessor;
import im.actor.core.modules.groups.GroupsProcessor;
import im.actor.core.modules.presence.PresenceProcessor;
import im.actor.core.modules.stickers.StickersProcessor;
import im.actor.core.modules.typing.TypingProcessor;
import im.actor.core.modules.messaging.MessagesProcessor;
import im.actor.core.modules.sequence.internal.CombinedDifference;
import im.actor.core.modules.sequence.internal.GetDiffCombiner;
import im.actor.core.modules.users.UsersProcessor;
import im.actor.core.network.parser.Update;
import im.actor.runtime.actors.messages.Void;
import im.actor.runtime.function.Function;
import im.actor.runtime.function.Predicate;
import im.actor.runtime.function.Tuple2;
import im.actor.runtime.promise.Promise;
import im.actor.runtime.promise.Promises;
import im.actor.runtime.promise.PromisesArray;

public class UpdateProcessor extends AbsModule {

    // Do Not Remove! WorkAround for missing j2objc translator include
    private static final Void DUMB = null;

    private MessagesProcessor messagesProcessor;

    // Seq Processors
    private final SequenceProcessor[] sequenceProcessors;

    // Weak Processors
    private final TypingProcessor typingProcessor;
    private final WeakProcessor[] weakProcessors;

    public UpdateProcessor(ModuleContext context) {
        super(context);

        this.messagesProcessor = new MessagesProcessor(context);
        sequenceProcessors = new SequenceProcessor[]{
                messagesProcessor,
                new UsersProcessor(context),
                new GroupsProcessor(context),
                new ContactsProcessor(context),
                new BlockListProcessor(context),
                new EncryptedProcessor(context),
                new StickersProcessor(context)
        };

        this.typingProcessor = new TypingProcessor(context);
        this.weakProcessors = new WeakProcessor[]{
                typingProcessor,
                new PresenceProcessor(context),
                new EventBusProcessor(context),
                new CallsProcessor(context)
        };
    }


    //
    // Entities Handling
    //

    public Promise<Void> applyRelated(List<ApiUser> users, List<ApiGroup> groups) {

        // Users

        Promise<Void> userPromise = PromisesArray.of(users)
                .map((Function<ApiUser, Promise<Tuple2<ApiUser, Boolean>>>) u -> users().containsAsync(u.getId())
                        .map(r -> new Tuple2<ApiUser, Boolean>(u, !r)))
                .filter(new Predicate<Tuple2<ApiUser, Boolean>>() {
                    @Override
                    public boolean apply(Tuple2<ApiUser, Boolean> apiUserBooleanTuple2) {
                        return apiUserBooleanTuple2.getT2();
                    }
                })
                .flatMap(r -> new User[]{new User(r.getT1())})
                .zip()
                .map(u -> {
                    if (u.size() > 0) {
                        users().addOrUpdateItems(u);
                    }
                    return null;
                });

        // Groups

        Promise<Void> groupsPromise = PromisesArray.of(groups)
                .map((Function<ApiGroup, Promise<Tuple2<ApiGroup, Boolean>>>) g -> groups().containsAsync(g.getId())
                        .map(r -> new Tuple2<ApiGroup, Boolean>(g, !r)))
                .filter(new Predicate<Tuple2<ApiGroup, Boolean>>() {
                    @Override
                    public boolean apply(Tuple2<ApiGroup, Boolean> apiGroupBooleanTuple2) {
                        return apiGroupBooleanTuple2.getT2();
                    }
                })
                .flatMap(r -> new Group[]{new Group(r.getT1())})
                .zip()
                .map(u -> {
                    if (u.size() > 0) {
                        groups().addOrUpdateItems(u);
                    }
                    return null;
                });

        return Promises.tuple(userPromise, groupsPromise)
                .map(v -> null);
    }


    //
    // Update Handling
    //

    public void processWeakUpdate(Update update, long date) {
        for (WeakProcessor w : weakProcessors) {
            if (w.process(update, date)) {
                return;
            }
        }
    }

    public Promise<Void> processUpdate(Update update) {

        // Small hack for stopping typing indicator
        if (update instanceof UpdateMessage) {
            UpdateMessage message = (UpdateMessage) update;
            typingProcessor.onMessage(message.getPeer(), message.getSenderUid());
        }

        for (SequenceProcessor sequenceProcessor : sequenceProcessors) {
            if (sequenceProcessor.process(update)) {
                return Promise.success(null);
            }
        }

        return Promise.success(null);
    }


    //
    // Difference
    //

    public void applyDifferenceUpdate(List<Update> updates) {

        CombinedDifference combinedDifference = GetDiffCombiner.buildDiff(updates);

        messagesProcessor.onDifferenceStart();

        for (Peer peer : combinedDifference.getReceived().keySet()) {
            long time = combinedDifference.getReceived().get(peer);
            messagesProcessor.onMessageReceived(buildApiPeer(peer), time);
        }

        for (Peer peer : combinedDifference.getRead().keySet()) {
            long time = combinedDifference.getRead().get(peer);
            messagesProcessor.onMessageRead(buildApiPeer(peer), time);
        }

        for (Peer peer : combinedDifference.getReadByMe().keySet()) {
            CombinedDifference.ReadByMeValue time = combinedDifference.getReadByMe().get(peer);
            messagesProcessor.onMessageReadByMe(buildApiPeer(peer), time.getDate(), time.getCounter());
        }

        for (Peer peer : combinedDifference.getMessages().keySet()) {
            messagesProcessor.onMessages(buildApiPeer(peer), combinedDifference.getMessages().get(peer));
        }

        for (Update u : combinedDifference.getOtherUpdates()) {
            processUpdate(u);
        }

        messagesProcessor.onDifferenceEnd();
    }
}