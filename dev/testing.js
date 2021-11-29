cljs.core.alter_meta_BANG_ = (function cljs$core$alter_meta_BANG_(var_args) {
    var args__4824__auto__ = [];
    var len__4818__auto___53420 = arguments.length;
    var i__4819__auto___53421 = (0);
    while (true) {
        if ((i__4819__auto___53421 < len__4818__auto___53420)) {
            args__4824__auto__.push((arguments[i__4819__auto___53421]));

            var G__53423 = (i__4819__auto___53421 + (1));
            i__4819__auto___53421 = G__53423;
            continue;
        } else {
        }
        break;
    }

    var argseq__4825__auto__ = ((((2) < args__4824__auto__.length)) ? (new cljs.core.IndexedSeq(args__4824__auto__.slice((2)), (0), null)) : null);
    return cljs.core.alter_meta_BANG_.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]), (arguments[(1)]), argseq__4825__auto__);
});

(cljs.core.alter_meta_BANG_.cljs$core$IFn$_invoke$arity$variadic = (function (iref, f, args) {
    return (iref.meta = cljs.core.apply.cljs$core$IFn$_invoke$arity$3(f, iref.meta, args));
}));

(cljs.core.alter_meta_BANG_.cljs$lang$maxFixedArity = (2));

/** @this {Function} */
(cljs.core.alter_meta_BANG_.cljs$lang$applyTo = (function (seq46099) {
    var G__46100 = cljs.core.first(seq46099);
    var seq46099__$1 = cljs.core.next(seq46099);
    var G__46101 = cljs.core.first(seq46099__$1);
    var seq46099__$2 = cljs.core.next(seq46099__$1);
    var self__4805__auto__ = this;
    return self__4805__auto__.cljs$core$IFn$_invoke$arity$variadic(G__46100, G__46101, seq46099__$2);
}));

/**
 * Atomically resets the metadata for an atom
 */
cljs.core.reset_meta_BANG_ = (function cljs$core$reset_meta_BANG_(iref, m) {
    return (iref.meta = m);
});

(function () {
    (function () {
        user.arity = (function user$arity(var_args) {
            var G__204013 = arguments.length;
            switch (G__204013) {
                case 0:
                    return user.arity.cljs$core$IFn$_invoke$arity$0();

                    break;
                case 1:
                    return user.arity.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

                    break;
                default:
                    throw (new Error(["Invalid arity: ", cljs.core.str.cljs$core$IFn$_invoke$arity$1(arguments.length)].join('')));

            }
        }); return (
            new cljs.core.Var(function () { return user.arity; }, new cljs.core.Symbol("user", "arity", "user/arity", -181980755, null), cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null, "ns", "ns", 441598760), new cljs.core.Keyword(null, "name", "name", 1843675177), new cljs.core.Keyword(null, "file", "file", -1269645878), new cljs.core.Keyword(null, "end-column", "end-column", 1425389514), new cljs.core.Keyword(null, "top-fn", "top-fn", -2056129173), new cljs.core.Keyword(null, "source", "source", -433931539), new cljs.core.Keyword(null, "column", "column", 2078222095), new cljs.core.Keyword(null, "line", "line", 212345235), new cljs.core.Keyword(null, "end-line", "end-line", 1837326455), new cljs.core.Keyword(null, "arglists", "arglists", 1661989754), new cljs.core.Keyword(null, "doc", "doc", 1913296891), new cljs.core.Keyword(null, "test", "test", 577538877)], [new cljs.core.Symbol(null, "user", "user", -1122004413, null), new cljs.core.Symbol(null, "arity", "arity", -168024608, null), "user.cljs", 12, new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null, "variadic?", "variadic?", 584179762), false, new cljs.core.Keyword(null, "fixed-arity", "fixed-arity", 1586445869), 1, new cljs.core.Keyword(null, "max-fixed-arity", "max-fixed-arity", -690205543), 1, new cljs.core.Keyword(null, "method-params", "method-params", -980792179), new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.PersistentVector.EMPTY, new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null, "a", "a", -482876059, null)], null)], null), new cljs.core.Keyword(null, "arglists", "arglists", 1661989754), cljs.core.list(cljs.core.PersistentVector.EMPTY, new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null, "a", "a", -482876059, null)], null)), new cljs.core.Keyword(null, "arglists-meta", "arglists-meta", 1944829838), cljs.core.list(null, null)], null), "arity", 1, 1, 1, cljs.core.list(cljs.core.PersistentVector.EMPTY, new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null, "a", "a", -482876059, null)], null)), null, (cljs.core.truth_(user.arity) ? user.arity.cljs$lang$test : null)])));
    })()
        ;

    (user.arity.cljs$core$IFn$_invoke$arity$0 = (function () {
        return (cljs.core.alter_meta_BANG_ = (1));
    }));

    (user.arity.cljs$core$IFn$_invoke$arity$1 = (function (a) {
        return cljs.core.meta.call(null, new cljs.core.Var(function () { return cljs.core.alter_meta_BANG_; }, new cljs.core.Symbol("cljs.core", "alter-meta!", "cljs.core/alter-meta!", 574694262, null), cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null, "ns", "ns", 441598760), new cljs.core.Keyword(null, "name", "name", 1843675177), new cljs.core.Keyword(null, "file", "file", -1269645878), new cljs.core.Keyword(null, "end-column", "end-column", 1425389514), new cljs.core.Keyword(null, "top-fn", "top-fn", -2056129173), new cljs.core.Keyword(null, "column", "column", 2078222095), new cljs.core.Keyword(null, "line", "line", 212345235), new cljs.core.Keyword(null, "end-line", "end-line", 1837326455), new cljs.core.Keyword(null, "arglists", "arglists", 1661989754), new cljs.core.Keyword(null, "doc", "doc", 1913296891), new cljs.core.Keyword(null, "test", "test", 577538877)], [new cljs.core.Symbol(null, "cljs.core", "cljs.core", 770546058, null), new cljs.core.Symbol(null, "alter-meta!", "alter-meta!", 1510444945, null), "cljs/core.cljs", (18), new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null, "variadic?", "variadic?", 584179762), true, new cljs.core.Keyword(null, "fixed-arity", "fixed-arity", 1586445869), (2), new cljs.core.Keyword(null, "max-fixed-arity", "max-fixed-arity", -690205543), (2), new cljs.core.Keyword(null, "method-params", "method-params", -980792179), new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.list(new cljs.core.Symbol(null, "iref", "iref", -647796531, null), new cljs.core.Symbol(null, "f", "f", 43394975, null), new cljs.core.Symbol(null, "args", "args", -1338879193, null))], null), new cljs.core.Keyword(null, "arglists", "arglists", 1661989754), cljs.core.list(new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null, "iref", "iref", -647796531, null), new cljs.core.Symbol(null, "f", "f", 43394975, null), new cljs.core.Symbol(null, "&", "&", -2144855648, null), new cljs.core.Symbol(null, "args", "args", -1338879193, null)], null)), new cljs.core.Keyword(null, "arglists-meta", "arglists-meta", 1944829838), cljs.core.list(null)], null), (1), (10643), (10643), cljs.core.list(new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null, "iref", "iref", -647796531, null), new cljs.core.Symbol(null, "f", "f", 43394975, null), new cljs.core.Symbol(null, "&", "&", -2144855648, null), new cljs.core.Symbol(null, "args", "args", -1338879193, null)], null)), "Atomically sets the metadata for a namespace/var/ref/agent/atom to be:\n\n  (apply f its-current-meta args)\n\n  f must be free of side-effects", (cljs.core.truth_(cljs.core.alter_meta_BANG_) ? cljs.core.alter_meta_BANG_.cljs$lang$test : null)])));
    }));

    (user.arity.cljs$lang$maxFixedArity = 1);

    return new cljs.core.Var(function () { return user.arity; }, new cljs.core.Symbol("user", "arity", "user/arity", -181980755, null), cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null, "ns", "ns", 441598760), new cljs.core.Keyword(null, "name", "name", 1843675177), new cljs.core.Keyword(null, "file", "file", -1269645878), new cljs.core.Keyword(null, "end-column", "end-column", 1425389514), new cljs.core.Keyword(null, "top-fn", "top-fn", -2056129173), new cljs.core.Keyword(null, "source", "source", -433931539), new cljs.core.Keyword(null, "column", "column", 2078222095), new cljs.core.Keyword(null, "line", "line", 212345235), new cljs.core.Keyword(null, "end-line", "end-line", 1837326455), new cljs.core.Keyword(null, "arglists", "arglists", 1661989754), new cljs.core.Keyword(null, "doc", "doc", 1913296891), new cljs.core.Keyword(null, "test", "test", 577538877)], [new cljs.core.Symbol(null, "user", "user", -1122004413, null), new cljs.core.Symbol(null, "arity", "arity", -168024608, null), "user.cljs", 12, new cljs.core.PersistentArrayMap(null, 6, [new cljs.core.Keyword(null, "variadic?", "variadic?", 584179762), false, new cljs.core.Keyword(null, "fixed-arity", "fixed-arity", 1586445869), 1, new cljs.core.Keyword(null, "max-fixed-arity", "max-fixed-arity", -690205543), 1, new cljs.core.Keyword(null, "method-params", "method-params", -980792179), new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.PersistentVector.EMPTY, new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null, "a", "a", -482876059, null)], null)], null), new cljs.core.Keyword(null, "arglists", "arglists", 1661989754), cljs.core.list(cljs.core.PersistentVector.EMPTY, new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null, "a", "a", -482876059, null)], null)), new cljs.core.Keyword(null, "arglists-meta", "arglists-meta", 1944829838), cljs.core.list(null, null)], null), "arity", 1, 1, 1, cljs.core.list(cljs.core.PersistentVector.EMPTY, new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null, "a", "a", -482876059, null)], null)), null, (cljs.core.truth_(user.arity) ? user.arity.cljs$lang$test : null)]));
})()


(function() {
    var temp__5753__auto__ = cyrik.omni_trace.instrument.instrumented.call(null, new cljs.core.Symbol("user","arity","user/arity",-181980755,null), new cljs.core.Var(function() {
        return user.arity;
    }
    ,new cljs.core.Symbol("user","arity","user/arity",-181980755,null),cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"ns","ns",441598760), new cljs.core.Keyword(null,"name","name",1843675177), new cljs.core.Keyword(null,"file","file",-1269645878), new cljs.core.Keyword(null,"end-column","end-column",1425389514), new cljs.core.Keyword(null,"top-fn","top-fn",-2056129173), new cljs.core.Keyword(null,"source","source",-433931539), new cljs.core.Keyword(null,"column","column",2078222095), new cljs.core.Keyword(null,"line","line",212345235), new cljs.core.Keyword(null,"end-line","end-line",1837326455), new cljs.core.Keyword(null,"arglists","arglists",1661989754), new cljs.core.Keyword(null,"doc","doc",1913296891), new cljs.core.Keyword(null,"test","test",577538877)], [new cljs.core.Symbol(null,"user","user",-1122004413,null), new cljs.core.Symbol(null,"arity","arity",-168024608,null), "user.cljs", 12, new cljs.core.PersistentArrayMap(null,6,[new cljs.core.Keyword(null,"variadic?","variadic?",584179762), false, new cljs.core.Keyword(null,"fixed-arity","fixed-arity",1586445869), 1, new cljs.core.Keyword(null,"max-fixed-arity","max-fixed-arity",-690205543), 1, new cljs.core.Keyword(null,"method-params","method-params",-980792179), new cljs.core.PersistentVector(null,2,5,cljs.core.PersistentVector.EMPTY_NODE,[cljs.core.PersistentVector.EMPTY, new cljs.core.PersistentVector(null,1,5,cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Symbol(null,"a","a",-482876059,null)],null)],null), new cljs.core.Keyword(null,"arglists","arglists",1661989754), cljs.core.list(cljs.core.PersistentVector.EMPTY, new cljs.core.PersistentVector(null,1,5,cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Symbol(null,"a","a",-482876059,null)],null)), new cljs.core.Keyword(null,"arglists-meta","arglists-meta",1944829838), cljs.core.list(null, null)],null), "arity", 1, 1, 1, cljs.core.list(cljs.core.PersistentVector.EMPTY, new cljs.core.PersistentVector(null,1,5,cljs.core.PersistentVector.EMPTY_NODE,[new cljs.core.Symbol(null,"a","a",-482876059,null)],null)), null, (cljs.core.truth_(user.arity) ? user.arity.cljs$lang$test : null)])), "/Users/lukas/Workspace/clojure/omni-trace/src/cyrik/omni_trace/testing_ns.cljc", new cljs.core.PersistentArrayMap(null,1,[new cljs.core.Keyword("cyrik.omni-trace","workspace","cyrik.omni-trace/workspace",489471878), cyrik.omni_trace.instrument.workspace],null));
    if (cljs.core.truth_(temp__5753__auto__)) {
        var instrumented__134281__auto__ = temp__5753__auto__;
        (user.arity = instrumented__134281__auto__);

        return new cljs.core.Symbol("user","arity","user/arity",-181980755,null);
    } else {
        return null;
    }
}
)()

