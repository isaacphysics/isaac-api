(ns rutherford.interop)

(defmulti generate identity)

(defmacro defimpl [base & body]
  `(defmethod generate ~base [_#]
     (reify ~base
       ~@body)))
