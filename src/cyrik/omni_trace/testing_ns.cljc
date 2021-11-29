(ns cyrik.omni-trace.testing-ns)

(def coin-values
  {:quarter 0.25
   :dime 0.10
   :nickel 0.05
   :penny 1})

(defn- calc-coin-value
  [coins]
  (->> coins
       (keep coin-values)
       (apply +)))

(defn- round-to-pennies
  "Because floating-point inaccuracy"
  [v]
  (-> v
      (* 100)
      Math/round
      (/ 100.0)))

(defn- calc-change-to-return*
  [amount-to-return coins]
  (let [value (calc-coin-value coins)]
    (cond
      (= value amount-to-return) coins
      (> value amount-to-return) nil
      :else (->> coin-values
                 (map first)
                 (map #(conj coins %))
                 (keep #(calc-change-to-return* amount-to-return %))
                 first))))

(defn- calc-change-to-return
  [machine selection]
  (let [amount-to-return (-> machine
                             :coins-inserted
                             calc-coin-value
                             (- (:price selection))
                             round-to-pennies)]
    (calc-change-to-return* amount-to-return [])))

(defn- get-selection
  [machine button]
  (-> machine
      :inventory
      button))

(defn valid-selection
  [machine button]
  (when-let [selection (get-selection machine button)]
    (and (some-> selection :qty (>= 1))
         (>= (-> machine
                 :coins-inserted
                 calc-coin-value)
             (:price selection)))))


;; dispense item
;; decrement qty
;; return change

(defn process-transaction
  [machine button]
  (let [selection (get-selection machine button)]
    (-> machine
        (update-in [:inventory button :qty]
                   dec)
        (assoc :dispensed
               selection)
        (assoc :coins-returned
               (calc-change-to-return machine
                                      selection))
        (assoc :coins-inserted []))))

(defn show-err-message
  [machine]
  (assoc machine :err-msg true))

(def machine-init {:inventory {:a1 {:name :taco
                                    :price 0.85
                                    :qty 10}}
                   :coins-inserted []
                   :coins-returned []
                   :dispensed nil
                   :err-msg nil})

(defn insert-coin
  [machine coin]
  (update-in machine
             [:coins-inserted]
             conj
             coin))

(defn press-button
  [machine button]
  (if (valid-selection machine button)
    (process-transaction machine button)
    (show-err-message machine)))

(defn retrieve-dispensed
  [machine]
  [(:dispensed machine)
   (dissoc machine :dispensed)])

(defn retrieve-change-returned
  [machine]
  [(:change-returned machine)
   (throw #?(:cljs (js/Error. "Oops") :clj (Exception. "Oops")))
   (dissoc machine :change-returned)])

(defn run-machine []
  (-> machine-init
      (insert-coin :quarter)
      (insert-coin :dime)
      (insert-coin :nickel)
      (insert-coin :penny)
      (press-button :a1)))

(defn i []
  (inc 1))