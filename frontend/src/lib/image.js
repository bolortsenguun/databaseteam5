// 그룹 수업 사진: 고른 파일을 긴 변 720px JPEG로 줄여 data URL로 만든다 (백엔드 상한 1,500,000자)
export const MAX_IMAGE_LENGTH = 1_500_000

export function resizeImage(file, maxSide = 720, quality = 0.82) {
  return new Promise((resolve, reject) => {
    if (!file || !file.type.startsWith('image/')) return reject(new Error('이미지 파일만 올릴 수 있어요.'))
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      const scale = Math.min(1, maxSide / Math.max(img.width, img.height))
      const canvas = document.createElement('canvas')
      canvas.width = Math.round(img.width * scale)
      canvas.height = Math.round(img.height * scale)
      canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height)
      URL.revokeObjectURL(url)
      const data = canvas.toDataURL('image/jpeg', quality)
      if (data.length > MAX_IMAGE_LENGTH) return reject(new Error('사진이 너무 커요. 다른 사진을 골라 주세요.'))
      resolve(data)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('사진을 읽지 못했어요. 다른 파일을 골라 주세요.'))
    }
    img.src = url
  })
}
