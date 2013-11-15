(ns rutherford.interop)

(defmulti generate (fn [interface] interface))
(defmulti construct (fn [type this] type))

(defmacro defimpl [base & body]
 `(defmethod generate ~base [_#]
    (let [this# (reify ~base
                  ~@body)]
      (construct ~base this#)
      this#)))

(defmacro defconstruct [type [this] & body]
  `(defmethod construct ~type [~type ~this]
     ~@body))