import { useCallback, useEffect, useRef, useState } from 'react'

export function useAsync(fn, deps = []) {
  const [state, setState] = useState({ loading: true, error: null, data: null })
  const seq = useRef(0)
  const run = useCallback(async () => {
    const id = ++seq.current
    setState(s => ({ ...s, loading: true, error: null }))
    try {
      const data = await fn()
      if (id === seq.current) setState({ loading: false, error: null, data })
      return data
    } catch (error) {
      if (id === seq.current) setState({ loading: false, error, data: null })
      return null
    }
  }, deps)
  useEffect(() => {
    run()
  }, [run])
  return { ...state, reload: run, setData: data => setState(s => ({ ...s, data })) }
}

export function useSafeAsync(fn, fallback, deps = []) {
  return useAsync(async () => {
    try {
      return await fn()
    } catch (e) {
      if (e?.code === 'UNAUTHORIZED') throw e
      return fallback
    }
  }, deps)
}
