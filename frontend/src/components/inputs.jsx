import { onlyDigits } from '../lib/format'

export function PhoneInput({ value, onChange, ...rest }) {
  const d = onlyDigits(value).slice(0, 11)
  const display = d.length <= 3 ? d : d.length <= 7 ? `${d.slice(0, 3)}-${d.slice(3)}` : `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`
  return (
    <input
      className="input"
      inputMode="numeric"
      autoComplete="tel"
      placeholder="010-0000-0000"
      value={display}
      onChange={e => onChange(onlyDigits(e.target.value).slice(0, 11))}
      {...rest}
    />
  )
}

export const phoneError = p => (/^010\d{8}$/.test(p || '') ? '' : '010으로 시작하는 휴대폰 번호 11자리를 입력해 주세요.')
export const passwordError = p => (!p || p.length < 8 ? '비밀번호는 8자 이상으로 정해 주세요.' : p.length > 50 ? '비밀번호는 50자 이하로 정해 주세요.' : '')
export const loginIdError = v => (/^[A-Za-z0-9_]{4,20}$/.test(v || '') ? '' : '아이디는 영문, 숫자, 밑줄(_) 4~20자로 정해 주세요.')
